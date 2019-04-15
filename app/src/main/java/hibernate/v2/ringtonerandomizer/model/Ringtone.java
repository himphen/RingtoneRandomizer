package hibernate.v2.ringtonerandomizer.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Ringtone {
	private String uriId;
	private String name;
	private String path;
	private Boolean isChecked = false;

	public String getUriId() {
		return uriId;
	}

	public void setUriId(String string) {
		this.uriId = string;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Nullable
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Boolean getChecked() {
		return isChecked;
	}

	public void setChecked(Boolean checked) {
		isChecked = checked;
	}

	@NonNull
	@Override
	public String toString() {
		return "Ringtone{" +
				"uriId='" + uriId + '\'' +
				", name='" + name + '\'' +
				", path='" + path + '\'' +
				", isChecked=" + isChecked +
				'}';
	}

}
