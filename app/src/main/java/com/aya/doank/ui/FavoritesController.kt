    private fun showEdit(cat: String, i: Int) {
        val f = store.all(cat).getOrNull(i) ?: return
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_fav, null)
        val nameEt = v.findViewById<EditText>(R.id.e_name)
        val latEt = v.findViewById<EditText>(R.id.e_lat)
        val lngEt = v.findViewById<EditText>(R.id.e_lng)
        val errTv = v.findViewById<TextView>(R.id.e_err)
        nameEt.setText(f.name)
        latEt.setText(f.lat.toString())
        lngEt.setText(f.lng.toString())

        val d = AlertDialog.Builder(activity, R.style.Theme_AYA_Dialog)
            .setView(v)
            .create()
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        widen(d)   // ← 92% lebar layar, senada dialog utama

        v.findViewById<View>(R.id.e_cancel).setOnClickListener { d.dismiss() }
        v.findViewById<View>(R.id.e_save).setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                errTv.text = "Nama tidak boleh kosong."
                errTv.visibility = View.VISIBLE
                return@setOnClickListener
            }
            val lat = latEt.text.toString().replace(',', '.').toDoubleOrNull()
            val lng = lngEt.text.toString().replace(',', '.').toDoubleOrNull()
            if (lat == null || lng == null || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                errTv.text = "Koordinat tidak valid (lat -90..90, lng -180..180)."
                errTv.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (store.updateAt(cat, i, name, lat, lng)) {
                d.dismiss()
                refreshDialogIfOpen()
                Toast.makeText(activity, "\"$name\" diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                errTv.text = "Nama sudah dipakai lokasi lain di kategori ini."
                errTv.visibility = View.VISIBLE
            }
        }
        d.show()
    }
