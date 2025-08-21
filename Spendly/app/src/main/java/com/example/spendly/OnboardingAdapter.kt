package com.example.spendly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val images: IntArray,
    private val titles: Array<String>,
    private val descriptions: Array<String>
) : RecyclerView.Adapter<OnboardingAdapter.PagerViewHolder>() {

    inner class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.onboardingImage)
        val itemTitle: TextView = itemView.findViewById(R.id.onboardingTitle)
        val itemDesc: TextView = itemView.findViewById(R.id.onboardingDesc)
        val imageContainer: View = itemView.findViewById(R.id.imageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        return PagerViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.onboarding_item, parent, false
            )
        )
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.itemTitle.text = ""
        holder.itemDesc.text = ""

        ImageScaler.setResourceSafely(holder.itemView.context, holder.itemImage, images[position])
        holder.itemTitle.text = titles[position]
        holder.itemDesc.text = descriptions[position]

        holder.itemView.tag = position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    init {
        setHasStableIds(true)
    }
}