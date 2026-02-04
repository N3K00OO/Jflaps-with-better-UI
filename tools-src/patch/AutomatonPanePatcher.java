package patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AutomatonPanePatcher {
  private static final String TARGET_METHOD_DESC = "(Ljava/awt/Graphics;)V";

  private AutomatonPanePatcher() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: patch.AutomatonPanePatcher <path-to-AutomatonPane.class>");
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
        if (!TARGET_METHOD_DESC.equals(descriptor)) {
          return mv;
        }
        if (!"paintComponent".equals(name) && !"printComponent".equals(name)) {
          return mv;
        }

        return new MethodVisitor(Opcodes.ASM9, mv) {
          @Override
          public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDesc) {
            if (opcode == Opcodes.GETSTATIC
              && "java/awt/Color".equals(owner)
              && "white".equals(fieldName)
              && "Ljava/awt/Color;".equals(fieldDesc)) {
              super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "launcher/CanvasColors",
                "canvasFillColor",
                "()Ljava/awt/Color;",
                false
              );
              return;
            }

            super.visitFieldInsn(opcode, owner, fieldName, fieldDesc);
          }
        };
      }
    };

    reader.accept(visitor, 0);
    byte[] patched = writer.toByteArray();

    Files.write(classFile, patched);
  }
}

