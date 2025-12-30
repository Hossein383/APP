package com.v2ray.ang.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemRecyclerMainBinding
import com.v2ray.ang.handler.MmkvManager

class MainRecyclerAdapter(val mActivity: MainActivity) : RecyclerView.Adapter<MainRecyclerAdapter.MainViewHolder>() {

    var isRunning = false

    override fun getItemCount() = mActivity.mainViewModel.serversCache.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return MainViewHolder(ItemRecyclerMainBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val guid = mActivity.mainViewModel.serversCache[position].guid ?: ""
        val isSelected = guid == (MmkvManager.getSelectServer() ?: "")
        updateUI(holder, isSelected)

        holder.itemView.setOnClickListener {
            if (guid.isNotEmpty() && guid != MmkvManager.getSelectServer()) {
                setSelectServer(guid)
            }
        }
    }

    fun setSelectServer(guid: String) {
        val currentSelect = MmkvManager.getSelectServer() ?: ""
        val oldPos = mActivity.mainViewModel.getPosition(currentSelect)
        val newPos = mActivity.mainViewModel.getPosition(guid)
        
        // ذخیره سرور جدید
        MmkvManager.setSelectServer(guid)
        
        // بروزرسانی گرافیکی لیست
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
        
        if (newPos >= 0) {
            mActivity.scrollToPositionCentered(newPos)
        }

        // حل مشکل سوییچ: اگر متصل هستیم، ری‌استارت کن تا سرور جدید اعمال شود
        if (mActivity.mainViewModel.isRunning.value == true) {
            mActivity.restartV2Ray()
        }
    }

    private fun updateUI(holder: MainViewHolder, isSelected: Boolean) {
        val binding = holder.itemMainBinding
        binding.layoutIndicator.clearAnimation()

        binding.ivStatusIcon.setImageResource(R.drawable.ic_server_idle)
        binding.ivStatusIcon.setColorFilter(if (isSelected) Color.WHITE else Color.LTGRAY)

        val serverData = mActivity.mainViewModel.serversCache.getOrNull(holder.layoutPosition)
        binding.tvName.text = serverData?.profile?.remarks ?: "Server"

        if (isSelected) {
            binding.layoutIndicator.setBackgroundResource(R.drawable.bg_server_active)
            binding.layoutIndicator.backgroundTintList = null
            binding.tvName.setTextColor(Color.parseColor("#00E5FF"))
            binding.tvName.maxLines = 2

            binding.layoutIndicator.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300)
                .setInterpolator(OvershootInterpolator()).start()

            val pulseAnim = AlphaAnimation(0.7f, 1.0f).apply {
                duration = 800
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            binding.layoutIndicator.startAnimation(pulseAnim)
        } else {
            binding.layoutIndicator.setBackgroundResource(R.drawable.bg_glass_input)
            binding.layoutIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
            binding.tvName.setTextColor(Color.WHITE)
            binding.tvName.maxLines = 1
            binding.layoutIndicator.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
        }
    }

    class MainViewHolder(val itemMainBinding: ItemRecyclerMainBinding) : RecyclerView.ViewHolder(itemMainBinding.root)
}
