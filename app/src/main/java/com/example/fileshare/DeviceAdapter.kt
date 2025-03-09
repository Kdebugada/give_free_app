package com.example.fileshare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fileshare.databinding.ItemDeviceBinding

class DeviceAdapter(private val onDeviceClick: (Device) -> Unit) : 
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    
    private val devices = mutableListOf<Device>()
    
    fun updateDevices(newDevices: List<Device>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }
    
    override fun getItemCount() = devices.size
    
    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(device: Device) {
            binding.deviceNameText.text = device.name
            binding.deviceAddressText.text = device.address
            
            binding.root.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }
} 