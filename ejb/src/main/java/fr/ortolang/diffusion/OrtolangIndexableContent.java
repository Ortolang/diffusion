package fr.ortolang.diffusion;

public class OrtolangIndexableContent {
	
	private StringBuffer sb;

    public OrtolangIndexableContent() {
        sb = new StringBuffer();
    }

     public void addContentPart(String content) {
        sb.append(content + " ");
    }

    @Override
    public String toString() {
        return sb.toString();
    }

}
