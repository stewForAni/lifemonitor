package com.life.gy

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.life.jv.ActivityLifeClassVisitor
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

public class LifeTransform extends Transform {
    @Override
    String getName() {
        return "LifeTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.PROJECT_ONLY
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        Collection<TransformInput> transformInputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        if (outputProvider != null) {
            outputProvider.deleteAll()
        }


        transformInputs.each {

            //这里对jar的处理，是因为gradle 3.6.0以上R类不会转为.class文件而会转成jar，因此在Transform实现中需要单独拷贝
            it.jarInputs.each {
                File file = it.file
                def dest = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.JAR)
                FileUtils.copyFile(file, dest)
            }

            it.directoryInputs.each {

                File dir = it.file
                if (dir.isDirectory()) {
                    dir.eachFileRecurse {
                        if(it.name.endsWith("Activity.class")&&!it.name.contains('Base')){
                            ClassReader classReader = new ClassReader(it.bytes)
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                            ClassVisitor classVisitor = new ActivityLifeClassVisitor(classWriter)
                            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                            byte[] bytes = classWriter.toByteArray()
                            FileOutputStream outputStream = new FileOutputStream(it.path)
                            outputStream.write(bytes)
                            outputStream.close()
                        }
                    }
                }

                def dest = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(it.file, dest)

            }
        }
    }
}