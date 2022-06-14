package com.example.gsoc.model;
import javax.persistence.*;

@Entity
@Table(name = "plugins")
public class Plugin {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "scm")
	private String scm;
	
	@Column(name = "release_timestamp")
	private String releaseTimestamp;
	
	public Plugin() {

	}

	public Plugin(String name, String scm, String releaseTimestamp) {
		this.name = name;
		this.releaseTimestamp = releaseTimestamp;
		this.scm = scm;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getScm() {
		return scm;
	}

	public void setScm(String scm) {
		this.scm = scm;
	}

	public String getReleaseTimestamp() {
		return releaseTimestamp;
	}

	public void setReleaseTimestamp(String releaseTimestamp) {
		this.releaseTimestamp = releaseTimestamp;
	}

	@Override
	public String toString() {
		return "Plugin [id=" + id + ", name=" + name + ", scm=" + scm + ", releaseTimestamp=" + releaseTimestamp + "]";
	}

}
