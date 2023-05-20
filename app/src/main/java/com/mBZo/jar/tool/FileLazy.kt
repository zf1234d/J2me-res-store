package com.mBZo.jar.tool

import java.io.File

class FileLazy(path: String){
    init {
        //自动创建
        if (File(path.substringBeforeLast("/")).exists().not()){
            File(path.substringBeforeLast("/")).mkdirs()
        }
        if (path.endsWith("/").not()){
            if (File(path).exists().not()){
                File(path).writeText("")
            }
        }
    }

    //文件操作
    private val file: File = File(path)
    fun writeNew(content: String = ""){
        file.writeText(content)
    }
    fun writeAddonStart(content: String){
        file.writeText("${content}${file.readText()}")
    }
    fun writeReplace(oldValue: String,newValue: String){
        file.writeText(file.readText().replace(oldValue,newValue))
    }
    fun writeRemove(content: String){
        file.writeText(file.readText().replace(content,""))
    }
    fun read(): String {
        return file.readText()
    }
    fun readLines(): List<String> {
        return file.readLines()
    }
    fun listFiles(): Array<out File>? {
        return file.listFiles()
    }
}