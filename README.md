# PicSave
#### 整理markdown文件中的图像路径工具
将markdown文件中的图像绝对路径与网络路径全部转换为相对路径
方便将typora等软件的markdown文件整理起来
#### 具体过程
 * 如下命令行传入参数：文件夹的路径（如myNotes） ，运行java程序即可
 ```
 javac PicSave.java
 java PicSave myNotes
 ```
 * 程序原理: 对于给定的文件夹，将文件夹中的md文件中的图像，无论是本地路径还是url路径，经过程序处理后会在md文件所在目录下生成一个image文件夹，并复制图像到image下，同时更改md中的链接为相对路径
