package patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CurvedArrowPatcher {
  private CurvedArrowPatcher() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: patch.CurvedArrowPatcher <path-to-CurvedArrow.class>");
      System.exit(2);
      return;
    }

    Path classFile = Paths.get(args[0]);
    byte[] original = Files.readAllBytes(classFile);

    ClassReader reader = new ClassReader(original);
    ClassWriter writer = new ClassWriter(reader, 0);

    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (!"drawText".equals(name) || !"(Ljava/awt/Graphics2D;)V".equals(descriptor)) {
          return mv;
        }

        return new MethodVisitor(Opcodes.ASM9, mv) {
          private int graphicsVarIndex = -1;
          private boolean sawGraphicsCast = false;
          private boolean patched = false;

          @Override
          public void visitTypeInsn(int opcode, String type) {
            super.visitTypeInsn(opcode, type);

            if (opcode == Opcodes.CHECKCAST && "java/awt/Graphics2D".equals(type)) {
              sawGraphicsCast = true;
            } else if (opcode != Opcodes.NEW) {
              sawGraphicsCast = false;
            }
          }

          @Override
          public void visitVarInsn(int opcode, int var) {
            if (opcode == Opcodes.ASTORE && sawGraphicsCast) {
              graphicsVarIndex = var;
              sawGraphicsCast = false;
            } else if (opcode != Opcodes.ALOAD) {
              sawGraphicsCast = false;
            }

            super.visitVarInsn(opcode, var);
          }

          @Override
          public void visitMethodInsn(int opcode, String owner, String methodName, String methodDesc, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface);

            if (patched) {
              return;
            }

            if (graphicsVarIndex < 0) {
              return;
            }

            if (opcode == Opcodes.INVOKEVIRTUAL
              && "java/awt/Graphics2D".equals(owner)
              && "setColor".equals(methodName)
              && "(Ljava/awt/Color;)V".equals(methodDesc)) {
              super.visitVarInsn(Opcodes.ALOAD, graphicsVarIndex);
              super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "launcher/CanvasColors",
                "canvasLineColor",
                "()Ljava/awt/Color;",
                false
              );
              super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/awt/Graphics2D",
                "setColor",
                "(Ljava/awt/Color;)V",
                false
              );
              patched = true;
            }
          }
        };
      }
    };

    reader.accept(visitor, 0);
    byte[] patched = writer.toByteArray();

    Files.write(classFile, patched);
  }
}

