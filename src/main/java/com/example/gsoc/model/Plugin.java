package com.example.gsoc.model;
import java.time.ZonedDateTime;

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
	private ZonedDateTime releaseTimestamp;
	
	public Plugin() {

	}

	public Plugin(String name, String scm, ZonedDateTime releaseTimestamp) {
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

	public ZonedDateTime getReleaseTimestamp() {
		return releaseTimestamp;
	}

	public void setReleaseTimestamp(ZonedDateTime releaseTimestamp) {
		this.releaseTimestamp = releaseTimestamp;
	}

	@Override
	public String toString() {
		return "Plugin [id=" + id + ", name=" + name + ", scm=" + scm + ", releaseTimestamp=" + releaseTimestamp + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((releaseTimestamp == null) ? 0 : releaseTimestamp.hashCode());
		result = prime * result + ((scm == null) ? 0 : scm.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Plugin other = (Plugin) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (releaseTimestamp == null) {
			if (other.releaseTimestamp != null)
				return false;
		} else if (!releaseTimestamp.equals(other.releaseTimestamp))
			return false;
		if (scm == null) {
			if (other.scm != null)
				return false;
		} else if (!scm.equals(other.scm))
			return false;
		return true;
	}

}
