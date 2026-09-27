package com.sys.update

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

object ContactsHelper {

    data class Contact(
        val name: String,
        val phones: List<String>,
        val emails: List<String>
    )

    data class Email(
        val name: String,
        val email: String
    )

    /**
     * قراءة كل جهات الاتصال
     */
    fun getAllContacts(ctx: Context): List<Contact> {
        val result = mutableListOf<Contact>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            )

            val cursor: Cursor? = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use { c ->
                val idxName = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val idxNum = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val idxId = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                val map = LinkedHashMap<String, MutableList<String>>()
                val nameMap = HashMap<String, String>()

                while (c.moveToNext()) {
                    val name = c.getString(idxName) ?: "Unknown"
                    val num = c.getString(idxNum) ?: continue
                    val id = c.getString(idxId) ?: name

                    nameMap[id] = name
                    map.getOrPut(id) { mutableListOf() }.add(num)
                }

                for ((id, numbers) in map) {
                    val name = nameMap[id] ?: "Unknown"
                    val emails = getEmailsForContact(ctx, id)
                    result.add(Contact(name, numbers.distinct(), emails))
                }
            }

            // جهات الاتصال بدون أرقام (الإيميل فقط)
            val emailOnly = getAllEmails(ctx)
            val namesWithPhones = result.map { it.name.lowercase() }.toSet()
            for (e in emailOnly) {
                if (e.name.lowercase() !in namesWithPhones) {
                    result.add(Contact(e.name, emptyList(), listOf(e.email)))
                }
            }

        } catch (e: Exception) {
            Log.e("ContactsHelper", "getAllContacts err: ${e.message}")
        }

        return result
    }

    /**
     * قراءة إيميلات جهة اتصال معينة
     */
    private fun getEmailsForContact(ctx: Context, contactId: String): List<String> {
        val emails = mutableListOf<String>()
        try {
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )
            cursor?.use { c ->
                val idx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (c.moveToNext()) {
                    val e = c.getString(idx) ?: continue
                    if (e.isNotBlank()) emails.add(e)
                }
            }
        } catch (_: Exception) {}
        return emails.distinct()
    }

    /**
     * قراءة كل الإيميلات في دفتر الهاتف
     */
    fun getAllEmails(ctx: Context): List<Email> {
        val result = mutableListOf<Email>()
        try {
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                null, null,
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME + " ASC"
            )
            cursor?.use { c ->
                val idxName = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
                val idxEmail = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (c.moveToNext()) {
                    val name = c.getString(idxName) ?: "Unknown"
                    val email = c.getString(idxEmail) ?: continue
                    if (email.isNotBlank()) result.add(Email(name, email))
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsHelper", "getAllEmails err: ${e.message}")
        }
        return result.distinctBy { it.email }
    }
}
