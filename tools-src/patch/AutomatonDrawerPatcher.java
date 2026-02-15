package patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AutomatonDrawerPatcher {
  private static final String TARGET_METHOD = "drawSelectionBox";
  private static final String TARGET_DESC = "(Ljava/awt/Graphics;)V";

  private AutomatonDrawerPatcher() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: patch.AutomatonDrawerPatcher <path-to-AutomatonDrawer.class>");
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
        if (TARGET_METHOD.equals(name) && TARGET_DESC.equals(descriptor)) {
          return new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String methodName, String methodDesc, boolean isInterface) {
              if (opcode == Opcodes.INVOKEVIRTUAL
                && "java/awt/Graphics".equals(owner)
                && "drawRect".equals(methodName)
                && "(IIII)V".equals(methodDesc)) {
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitMethodInsn(
                  Opcodes.INVOKESTATIC,
                  "launcher/CanvasColors",
                  "selectionBoxColor",
                  "()Ljava/awt/Color;",
                  false
                );
                super.visitMethodInsn(
                  Opcodes.INVOKEVIRTUAL,
                  "java/awt/Graphics",
                  "setColor",
                  "(Ljava/awt/Color;)V",
                  false
                );
              }
              super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface);
            }
          };
        }
        return mv;
      }
    };

    reader.accept(visitor, 0);
    byte[] patched = writer.toByteArray();

    Files.write(classFile, patched);
  }
}