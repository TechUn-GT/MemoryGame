package com.techun.memorygame.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.techun.memorygame.MemoryItem
import com.techun.memorygame.databinding.ItemCardBinding
import com.techun.memorygame.utils.extensions.loadByResource
import com.wajahatkarim3.easyflipview.EasyFlipView


class CardAdapter(
    private var boardList: List<MemoryItem>,
    private val onItemSelected: OnItemSelected? = null
) : RecyclerView.Adapter<CardAdapter.BoardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        return BoardViewHolder(
            ItemCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), onItemSelected
        )
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        holder.bind(boardList[position])
    }

    override fun getItemCount() = boardList.size

    inner class BoardViewHolder(
        val binding: ItemCardBinding,
        private val onItemSelected: OnItemSelected? = null
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: MemoryItem) = with(binding.root) {
            if (binding.flipView.currentFlipState === EasyFlipView.FlipState.FRONT_SIDE && card.status) {
                binding.flipView.flipDuration = 0
                binding.flipView.flipTheView()
                binding.imgPreview.loadByResource(card.urlImagen)
            } else if (binding.flipView.currentFlipState === EasyFlipView.FlipState.BACK_SIDE && !card.status) {
                binding.flipView.flipDuration = 0
                binding.flipView.flipTheView()
            }
            binding.flipView.setOnClickListener {
                boardList[adapterPosition].status = !card.status
                binding.flipView.flipDuration = 700
                binding.flipView.flipTheView()
            }
        }
    }

    interface OnItemSelected {
        fun onClickListener(
            imageFront: ImageView,
            imageBack: ImageView,
            position: String,
            adapterPosition: Int
        )
    }
}