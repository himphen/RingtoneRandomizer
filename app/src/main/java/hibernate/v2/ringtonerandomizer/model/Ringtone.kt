package hibernate.v2.ringtonerandomizer.model

data class Ringtone(
        var uriId: String,
        var name: String,
        var path: String? = null,
        var checked: Boolean = false
) {
    override fun toString(): String {
        return "Ringtone{" +
                "uriId='" + uriId + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", isChecked=" + checked +
                '}'
    }

    companion object {
        const val PATH_INTERNAL_STORAGE = "Internal Storage"
    }
}