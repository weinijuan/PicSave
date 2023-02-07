import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * 将绝对路径与网络路径全部转换为相对路径
 *
 * 命令行需要传入参数 文件夹的路径
 * 具体操作: 对于给定的文件夹，将文件夹中的md文件中的图像，无论是本地路径还是url路径，
 * 经过程序处理后会在md文件所在目录下生成一个image文件夹，并复制图像到image下，
 * 同时更改md中的链接为相对路径
 */
public class PicSave
{
    private static ArrayList<File> files = new ArrayList<>();

    /***
     * 获取指定目录下的所有文件File
     * @param file 指定目录
     */
    public static void getAllFile(File file)
    {
        if (file.isDirectory())
        {
            File[] files1 = file.listFiles();
            for (File k : files1)
            {
                getAllFile(k);
            }
        } else
        {
            if (file.getName().contains(".md") || file.getName().contains(".markdown"))
            {
                files.add(file);
            }
        }
    }

    /***
     * 得到第一个正则表达式和第二个正则表达式中间的字符串们
     * @param src 字符串源
     * @param startReg  .
     * @param endReg .
     * @return
     */
    public static Set<String> find(String src, String startReg, String endReg)
    {
        Set<String> picList = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile(startReg);
        Matcher matcher = pattern.matcher(src);
        int start = 0;
        while (matcher.find(start))
        {
            int pathStart = matcher.end();
            int pathEnd = pathStart;
            if (pathStart != src.length() - 1)
            {
                Matcher matcherEnd = Pattern.compile(endReg).matcher(src);
                if (matcherEnd.find(pathStart + 1))
                {
                    pathEnd = matcherEnd.start();
                }
            }
            if (pathEnd != pathStart)
            {
                picList.add(src.substring(pathStart, pathEnd));
                start = pathEnd + 1;
            } else
            {
                break;
            }
        }
        return picList;
    }

    /***
     * 通过url下载图片保存到本地, 其中newPath指定文件名，无需加后缀
     * @param urlString url
     * @param newPath 新文件名
     * @throws Exception .
     */
    public static File download(String urlString, String newPath) throws Exception
    {
        // 构造URL
        URL url = new URL(urlString);
        // 打开连接
        URLConnection con = url.openConnection();
        // 得到图片类型
        Map headers = con.getHeaderFields();
        Set<String> keys = headers.keySet();
        String type = "";
        for (String key : keys)
        {
            if (key == null)
            {
                continue;
            }
            if (key.equals("Content-Type"))
            {
                String val = con.getHeaderField(key);
                int start = val.indexOf("image/");
                if (start != -1)
                {
                    type = val.substring(start + 6, val.length());
                    System.out.println(type);
                }
            }
        }
        if (type.length() != 0)
        {
            // 输入流
            InputStream is = con.getInputStream();
            // 1K的数据缓冲
            byte[] bs = new byte[1024];
            // 读取到的数据长度
            int len;
            // 输出的文件流
            String filename = newPath + "." + type;  //下载路径及下载图片名称
            File file = new File(filename);
            FileOutputStream os = new FileOutputStream(file);
            // 开始读取
            while ((len = is.read(bs)) != -1)
            {
                os.write(bs, 0, len);
            }
            // 完毕，关闭所有链接
            os.close();
            is.close();
            return file;
        }
        return null;
    }

    /***
     * 将file内容转为字符串
     * @param file 文件
     * @return string
     */
    public static String loadFile(File file)
    {
        Long fileLength = file.length();
        String str = "";
        if (fileLength > Integer.MAX_VALUE)
        {
            throw new RuntimeException("文件太大");
        }
        byte[] fileContent = new byte[fileLength.intValue()];
        try
        {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileContent);
            fileInputStream.close();
            str = new String(fileContent);
        } catch (FileNotFoundException e)
        {
            System.out.println("path is error");
            throw new RuntimeException(e);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return str;
    }

    /***
     * 在file所在处创建名为name的目录
     * @param file 当前文件
     * @param name 新目录名
     */
    public static void createDir(File file, String name)
    {
        String imagePath = file.getParent() + "\\"+name;
        new File(imagePath).mkdir();
    }

    public static void reWrite(File file, String src) throws IOException
    {
        FileWriter writer = new FileWriter(file);
        writer.write(src);
        writer.flush();
        writer.close();
    }

    /***
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            System.out.println("不合法参数");
            return;
        }
        File file = new File(args[0]);
        getAllFile(file);
        int i = 0;
        for (File mdFile : files)
        {
            String str = loadFile(mdFile);
            // 获取形如 <img src="">的
            Set<String> picList1 = find(str, "<img +src *= *\"", "\"");
            // 获取形如 ![]()的
            Set<String> picList2 = find(str, "!\\[\\S*\\]\\(", "\\)");
            if (!picList1.isEmpty() || !picList2.isEmpty())
            {
                createDir(mdFile, "image");
                for (String s : picList1)
                {
                    File srcFile = new File(s);
                    File destFile = new File(mdFile.getParent() + "/image/image" + i+".png");
                    if (destFile.exists())
                    {
                        if (!destFile.delete())
                        {
                            System.out.println("老图片文件无法删除");
                        }
                    }
                    Files.copy(srcFile.toPath(), destFile.toPath());
                    str = str.replace(s, "./image/" + destFile.getName());
                    reWrite(mdFile, str);
                    i++;
                }
                for (String s : picList2)
                {
                    // 使用http或https协议的网络图片
                    if (s.contains("http"))
                    {
                        File download = download(s, mdFile.getParent() + "/image" + i);
                        if (download != null)
                        {
                            str = str.replace(s, "./image/"+download.getName());
                            reWrite(mdFile, str);
                        }
                    }
                    // 放在本地 C/D/E/F盘的本地图片,和第一种方式操作一样
                    else if (s.contains("C:")|| s.contains("D:")||s.contains("E:")||s.contains("F" +
                            ":"))
                    {
                        File srcFile = new File(s);
                        File destFile = new File(mdFile.getParent() + "/image/image" + i+".png");
                        if (destFile.exists())
                        {
                            if (!destFile.delete())
                            {
                                System.out.println("老图片文件无法删除");
                            }
                        }
                        Files.copy(srcFile.toPath(), destFile.toPath());
                        str = str.replace(s, "./image/" + destFile.getName());
                        reWrite(mdFile, str);
                    }
                    i++;
                }
            }
        }
    }
}
