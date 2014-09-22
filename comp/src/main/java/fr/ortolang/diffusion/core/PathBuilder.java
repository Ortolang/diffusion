package fr.ortolang.diffusion.core;


public class PathBuilder {
	
	public static final String PATH_SEPARATOR = "/";
	public static final char PATH_SEPARATOR_CHAR = '/';
	private static final String VALID_PATH_REGEXP = PATH_SEPARATOR + "|" + PATH_SEPARATOR + "[a-zA-Z0-9\\-_.~=:&+$,]+(" + PATH_SEPARATOR + "[a-zA-Z0-9\\-_.~=:&+$,]+)*";

	private StringBuffer buffer;
	
	private PathBuilder() {
		buffer = new StringBuffer();
	}
	
	private PathBuilder(String path) throws InvalidPathException {
		this();
		this.path(path);
	}
	
	public static PathBuilder newInstance() {
		return new PathBuilder();
	}
	
	public static PathBuilder fromPath(String path) throws InvalidPathException {
		return new PathBuilder(path);
	}
	
	public PathBuilder path(String path) throws InvalidPathException {
		String part = normalize(path);
		if ( !part.equals(PATH_SEPARATOR) ) {
			buffer.append(part);
		}
		return this;
	}
	
	public String build() {
		if ( buffer.length() <= 1 ) {
			return PATH_SEPARATOR;
		} 
		return buffer.toString();
	}
	
	public String[] buildParts() {
		if ( buffer.length() <= 1 ) {
			return new String[0];
		} 
		return buffer.substring(1).split(PATH_SEPARATOR);
	}
	
	public boolean isRoot() {
		if ( buffer.length() <= 1 ) {
			return true;
		}
		return false;
	}
	
	public int depth() {
		if ( buffer.length() <= 1 ) {
			return 0;
		} 
		String path = buffer.toString();
		int depth = path.length() - path.replace(PATH_SEPARATOR, "").length();
		return depth;
	}
	
	public PathBuilder parent() {
		if ( buffer.length() >= 1 ) {
			buffer.delete(buffer.lastIndexOf(PATH_SEPARATOR), buffer.length());
		}
		if ( buffer.length() == 0 ) {
			buffer.append(PATH_SEPARATOR);
		}
		return this;
	}
	
	public PathBuilder relativize(String path) throws InvalidPathException {
		String parent = normalize(path);
		if (checkFiliation(parent, buffer.toString())) {
			buffer = buffer.delete(0, parent.length());
		} else {
			throw new InvalidPathException("path " + path + " is not parent");
		}
		return this;
	}
	
	public String part() {
		if ( buffer.length() <= 1 ) {
			return "";
		} 
		return buffer.substring(buffer.lastIndexOf(PATH_SEPARATOR)+1);
	}
	
	public boolean isChild(String path) throws InvalidPathException {
		return checkFiliation(normalize(path), buffer.toString());
	}
	
	public boolean isParent(String path) throws InvalidPathException {
		return checkFiliation(buffer.toString(), normalize(path));
	}
	
	private String normalize(String path) throws InvalidPathException {
        String[] paths = path.split(PATH_SEPARATOR);
        String[] newPaths = new String[paths.length];
        StringBuffer newPath = new StringBuffer();
        int index = 0;

        for (int i = 0; i < paths.length; i++) {
            if (!paths[i].equals("") && !paths[i].equals(".")) {
                if (paths[i].equals("..")) {
                    if (index > 0) {
                        index--;
                    }
                } else {
                    newPaths[index] = paths[i];
                    index++;
                }
            }
        }
        for (int i = 0; i < index; i++) {
            newPath.append(PATH_SEPARATOR + newPaths[i]);
        }
        //root case
        if (newPath.length() == 0) {
            newPath.append(PATH_SEPARATOR);
        }
        String npath = newPath.toString();
        valid(npath);
        return npath;
    }

	private void valid(String path) throws InvalidPathException {
        if (!path.matches(VALID_PATH_REGEXP)) {
            throw new InvalidPathException(path);
        }
    }
	
	private boolean checkFiliation(String parent, String children) {
		if ( parent.length() <= 1 ) { 
			return true;
		}
		if ( children.length() <= parent.length() ) {
			return false;
		}
		if ( !children.startsWith(parent) ) {
			return false;
		}
		if ( children.charAt(parent.length()) != PATH_SEPARATOR_CHAR ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PathBuilder other = (PathBuilder) obj;
		if (!buffer.toString().equals(other.buffer.toString()))
			return false;
		return true;
	}

	@Override
	public PathBuilder clone() {
		PathBuilder builder = new PathBuilder();
		builder.buffer = new StringBuffer().append(buffer);
		return builder;
	}

}