package patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SelectionDrawerPatcher {
  private static final String TARGET_FIELD = "SELECTED_COLOR";
  private static final String TARGET_DESC = "Ljava/awt/Color;";

  private SelectionDrawerPatcher() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: patch.SelectionDrawerPatcher <path-to-SelectionDrawer.class>");
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
        return new MethodVisitor(Opcodes.ASM9, mv) {
          @Override
          public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDesc) {
            if (opcode == Opcodes.GETSTATIC
              && "gui/viewer/SelectionDrawer".equals(owner)
              && TARGET_FIELD.equals(fieldName)
              && TARGET_DESC.equals(fieldDesc)) {
              super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "launcher/CanvasColors",
                "selectedNodeColor",
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