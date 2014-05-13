package fr.ortolang.diffusion;

public class OrtolangIndexablePlainTextContent {
	
	private StringBuffer sb;

    public OrtolangIndexablePlainTextContent() {
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
