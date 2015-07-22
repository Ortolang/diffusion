package fr.ortolang.diffusion.store.handle.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@IdClass(HandlePK.class)
@Table(name = "handles", indexes = { @Index(columnList = "handle, data") })
@NamedQueries({ 
	@NamedQuery(name = "findHandleByName", query = "SELECT h FROM Handle h WHERE h.handle = :name"),
	@NamedQuery(name = "findHandleNameForKey", query = "SELECT DISTINCT(h.handle) FROM Handle h WHERE h.key = :key")})
@SuppressWarnings("serial")
public class Handle implements Serializable {

	@Id
	private byte[] handle;
	@Id
	@Column(name = "idx")
	private int index;
	private String key;
	private byte[] type;
	private byte[] data;
	@Column(name = "ttl_type")
	private short ttlType;
	@Column(nullable = true)
	private int ttl;
	@Column(nullable = true)
	private int timestamp;
	@Column(name = "refs")
	@Type(type = "org.hibernate.type.TextType")
	private String references;
	@Column(name = "admin_read")
	private boolean adminRead;
	@Column(name = "admin_write")
	private boolean adminWrite;
	@Column(name = "pub_read")
	private boolean pubRead;
	@Column(name = "pub_write")
	private boolean pubWrite;

	public Handle() {
	}

	public Handle(byte[] handle, int index, String key, byte[] type, byte[] data, short ttlType, int ttl, int timestamp, String references, boolean adminRead, boolean adminWrite, boolean pubRead,
			boolean pubWrite) {
		super();
		this.handle = handle;
		this.index = index;
		this.key = key;
		this.type = type;
		this.data = data;
		this.ttlType = ttlType;
		this.ttl = ttl;
		this.timestamp = timestamp;
		this.references = references;
		this.adminRead = adminRead;
		this.adminWrite = adminWrite;
		this.pubRead = pubRead;
		this.pubWrite = pubWrite;
	}

	public byte[] getHandle() {
		return handle;
	}

	public void setHandle(byte[] handle) {
		this.handle = handle;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public byte[] getType() {
		return type;
	}

	public void setType(byte[] type) {
		this.type = type;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public short getTtlType() {
		return ttlType;
	}

	public void setTtlType(short ttlType) {
		this.ttlType = ttlType;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public String getRefs() {
		return references;
	}

	public void setRefs(String refs) {
		this.references = refs;
	}

	public boolean isAdminRead() {
		return adminRead;
	}

	public void setAdminRead(boolean adminRead) {
		this.adminRead = adminRead;
	}

	public boolean isAdminXrite() {
		return adminWrite;
	}

	public void setAdminXrite(boolean adminXrite) {
		this.adminWrite = adminXrite;
	}

	public boolean isPubRead() {
		return pubRead;
	}

	public void setPubRead(boolean pubRead) {
		this.pubRead = pubRead;
	}

	public boolean isPubWrite() {
		return pubWrite;
	}

	public void setPubWrite(boolean pubWrite) {
		this.pubWrite = pubWrite;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(new String(getHandle(), "UTF-8")).append(" ");
			sb.append(getIndex()).append(" ");
			sb.append(new String(getType(), "UTF-8")).append(" ");
			if ( !getType().equals("HS_ADMIN".getBytes())) {
				sb.append(new String(getData(), "UTF-8")).append(" ");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

}
