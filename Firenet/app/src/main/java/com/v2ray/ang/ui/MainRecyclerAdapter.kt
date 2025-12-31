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

    // تابعی برای لود مجدد و مطمئن لیست از ViewModel
    fun refreshData() {
        notifyDataSetChanged()
    }

    override fun getItemCount() = mActivity.mainViewModel.serversCache.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return MainViewHolder(
            ItemRecyclerMainBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val serverData = mActivity.mainViewModel.serversCache.getOrNull(position)
        val guid = serverData?.guid ?: ""
        val isSelected = guid == (MmkvManager.getSelectServer() ?: "")
        
        updateUI(holder, isSelected, serverData?.profile?.remarks ?: "Unknown Server")

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
        
        MmkvManager.setSelectServer(guid)
        
        // بروزرسانی فقط آیتم‌های تغییر یافته برای جلوگیری از لگ
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
        
        if (newPos >= 0) {
            mActivity.scrollToPositionCentered(newPos)
        }

        // سوئیچ آنی در صورت روشن بودن VPN
        if (mActivity.mainViewModel.isRunning.value == true) {
            mActivity.restartV2Ray()
        }
    }

    private fun updateUI(holder: MainViewHolder, isSelected: Boolean, serverName: String) {
        val binding = holder.itemMainBinding
        
        // ریست کردن انیمیشن‌ها برای جلوگیری از تداخل در اسکرول
        binding.root.clearAnimation()
        binding.root.animate().cancel()

        binding.tvName.text = serverName

        if (isSelected) {
            // استایل سرور انتخاب شده (نئون آبی)
            binding.root.setCardBackgroundColor(Color.parseColor("#3300D2FF")) // آبی نیمه شفاف
            binding.root.strokeColor = Color.parseColor("#00D2FF")
            binding.root.strokeWidth = 4 
            
            binding.tvName.setTextColor(Color.parseColor("#00D2FF"))
            binding.tvName.maxLines = 2

            // انیمیشن بزرگنمایی نرم
            binding.root.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .start()

            // انیمیشن ضربان (Pulse) برای حس زنده بودن
            val pulseAnim = AlphaAnimation(0.6f, 1.0f).apply {
                duration = 1000
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            binding.root.startAnimation(pulseAnim)
            
        } else {
            // استایل سرورهای معمولی (شیشه‌ای محو)
            binding.root.setCardBackgroundColor(Color.parseColor("#1AFFFFFF"))
            binding.root.strokeColor = Color.parseColor("#1AFFFFFF")
            binding.root.strokeWidth = 2
            
            binding.tvName.setTextColor(Color.WHITE)
            binding.tvName.maxLines = 1
            
            // بازگشت به اندازه اصلی
            binding.root.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
        }
    }

    class MainViewHolder(val itemMainBinding: ItemRecyclerMainBinding) : RecyclerView.ViewHolder(itemMainBinding.root)
}
