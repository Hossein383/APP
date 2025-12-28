package com.v2ray.ang.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemRecyclerMainBinding
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.V2RayServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainRecyclerAdapter(val activity: MainActivity) : RecyclerView.Adapter<MainRecyclerAdapter.BaseViewHolder>() {
    companion object {
        private const val VIEW_TYPE_ITEM = 1
    }

    private var mActivity: MainActivity = activity
    var isRunning = false
    private var switchJob: Job? = null

    override fun getItemCount() = mActivity.mainViewModel.serversCache.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (holder is MainViewHolder) {
            // استفاده از getOrNull برای جلوگیری از کرش در صورت خالی بودن لیست
            val serverData = mActivity.mainViewModel.serversCache.getOrNull(position) ?: return
            val guid = serverData.guid
            val profile = serverData.profile
            
            holder.itemMainBinding.tvName.text = profile.remarks
            val isSelected = (guid == MmkvManager.getSelectServer())

            // اعمال تغییرات بصری
            updateUI(holder, isSelected)

            holder.itemView.setOnClickListener {
                // ویبره لمسی
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                
                setSelectServer(guid, position)
                mActivity.scrollToPositionCentered(position)
            }
        }
    }

    private fun updateUI(holder: MainViewHolder, isSelected: Boolean) {
        val binding = holder.itemMainBinding
        
        if (isSelected) {
            // حالت انتخاب شده
            try {
                binding.layoutIndicator.setBackgroundResource(R.drawable.bg_server_active)
            } catch (e: Exception) {
                // اگر فایل drawable هنوز ساخته نشده، کرش نکند
                binding.layoutIndicator.setBackgroundColor(Color.BLUE)
            }
            
            binding.layoutIndicator.backgroundTintList = null 
            binding.ivStatusIcon.setImageResource(R.drawable.ic_server_active)
            binding.ivStatusIcon.setColorFilter(Color.WHITE)
            
            // رنگ متن را مستقیم ست میکنیم که اگر colorAccent نبود خطا ندهد
            binding.tvName.setTextColor(Color.parseColor("#00E5FF")) 
            binding.tvName.maxLines = 2

            binding.layoutIndicator.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(250)
                .setInterpolator(OvershootInterpolator())
                .start()
        } else {
            // حالت عادی
            binding.layoutIndicator.setBackgroundResource(R.drawable.bg_glass_input)
            binding.layoutIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
            binding.ivStatusIcon.setImageResource(R.drawable.ic_server_idle)
            binding.ivStatusIcon.setColorFilter(Color.LTGRAY)
            
            binding.tvName.setTextColor(Color.WHITE)
            binding.tvName.maxLines = 1

            binding.layoutIndicator.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start()
        }
    }

    fun setSelectServer(guid: String, position: Int) {
        val lastSelected = MmkvManager.getSelectServer()
        if (guid != lastSelected) {
            MmkvManager.setSelectServer(guid)
            
            if (!lastSelected.isNullOrEmpty()) {
                val oldPos = mActivity.mainViewModel.getPosition(lastSelected)
                if (oldPos != -1) notifyItemChanged(oldPos)
            }
            notifyItemChanged(position)

            if (isRunning) {
                switchJob?.cancel()
                switchJob = mActivity.lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        V2RayServiceManager.stopVService(mActivity)
                        delay(500)
                        V2RayServiceManager.startVService(mActivity)
                    } catch (e: Exception) {
                        Log.e(AppConfig.TAG, "Restart Error", e)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
         return MainViewHolder(ItemRecyclerMainBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemViewType(position: Int): Int = VIEW_TYPE_ITEM

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class MainViewHolder(val itemMainBinding: ItemRecyclerMainBinding) : BaseViewHolder(itemMainBinding.root)
}
