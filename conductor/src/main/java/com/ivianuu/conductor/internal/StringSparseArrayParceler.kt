package com.ivianuu.conductor.internal

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray

class StringSparseArrayParceler : Parcelable {

    val stringSparseArray: SparseArray<String>

    constructor(stringSparseArray: SparseArray<String>) {
        this.stringSparseArray = stringSparseArray
    }

    internal constructor(`in`: Parcel) {
        stringSparseArray = SparseArray()

        val size = `in`.readInt()

        for (i in 0 until size) {
            stringSparseArray.put(`in`.readInt(), `in`.readString())
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        val size = stringSparseArray.size()

        out.writeInt(size)

        for (i in 0 until size) {
            val key = stringSparseArray.keyAt(i)

            out.writeInt(key)
            out.writeString(stringSparseArray.get(key))
        }
    }

    companion object {
        @JvmStatic
        val CREATOR: Parcelable.Creator<StringSparseArrayParceler> =
            object : Parcelable.Creator<StringSparseArrayParceler> {
                override fun createFromParcel(`in`: Parcel): StringSparseArrayParceler {
                    return StringSparseArrayParceler(`in`)
                }

                override fun newArray(size: Int): Array<StringSparseArrayParceler> {
                    return newArray(size)
                }
            }
    }

}
