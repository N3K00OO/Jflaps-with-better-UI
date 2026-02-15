package patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class StateDrawerPatcher {
  private static final String TARGET_METHOD = "drawArea";
  private static final String TARGET_DESC = "(Ljava/awt/Graphics;Lautomata/Automaton;Lautomata/State;Ljava/awt/Point;Ljava/awt/Color;)V";

  private StateDrawerPatcher() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: patch.StateDrawerPatcher <path-to-StateDrawer.class>");
      System.exit(2);
      return;
    }

    Path classFile = Paths.get(args[0]);
    byte[] original = Files.readAllBytes(classFile);

    ClassReader reader = new ClassReader(original);
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (!TARGET_METHOD.equals(name) || !TARGET_DESC.equals(descriptor)) {
          return mv;
        }

        return new MethodVisitor(Opcodes.ASM9, mv) {
          private int drawOvalCount = 0;

          @Override
          public void visitMethodInsn(int opcode, String owner, String methodName, String methodDesc, boolean isInterface) {
            if (opcode == Opcodes.INVOKEVIRTUAL
              && "java/awt/Graphics".equals(owner)
              && "drawOval".equals(methodName)
              && "(IIII)V".equals(methodDesc)) {
              drawOvalCount++;
              if (drawOvalCount == 2) {
                super.visitMethodInsn(
                  Opcodes.INVOKESTATIC,
                  "launcher/CanvasColors",
                  "drawFinalStateMarkerOval",
                  "(Ljava/awt/Graphics;IIII)V",
                  false
                );
                return;
              }
            }

            if (opcode == Opcodes.INVOKEVIRTUAL
              && "java/awt/Graphics".equals(owner)
              && "fillPolygon".equals(methodName)
              && "([I[II)V".equals(methodDesc)) {
              super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "launcher/CanvasColors",
                "fillStartMarkerTriangle",
                "(Ljava/awt/Graphics;[I[II)V",
                false
              );
              return;
            }

            if (opcode == Opcodes.INVOKEVIRTUAL
              && "java/awt/Graphics".equals(owner)
              && "drawPolygon".equals(methodName)
              && "([I[II)V".equals(methodDesc)) {
              super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "launcher/CanvasColors",
                "drawStartMarkerTriangle",
                "(Ljava/awt/Graphics;[I[II)V",
                false
              );
              return;
            }

            super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface);
          }
        };
      }
    };

    reader.accept(visitor, 0);
    byte[] patched = writer.toByteArray();
    Files.write(classFile, patched);
  }
}