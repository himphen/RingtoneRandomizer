package hibernate.v2.ringtonerandomizer.model;

public class Ringtone {
	private String musicId;
	private String name;
	private String path;
	private String position;

	public String getMusicId() {
		return musicId;
	}

	public void setMusicId(String string) {
		this.musicId = string;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return "Ringtone{" +
				"musicId='" + musicId + '\'' +
				", name='" + name + '\'' +
				", path='" + path + '\'' +
				", position='" + position + '\'' +
				'}';
	}
}
