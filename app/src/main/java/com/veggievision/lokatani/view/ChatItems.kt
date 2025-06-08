package com.veggievision.lokatani.view

import android.view.View
import com.veggievision.lokatani.R
import com.veggievision.lokatani.databinding.ItemChatBotBinding
import com.veggievision.lokatani.databinding.ItemChatUserBinding
import com.xwray.groupie.viewbinding.BindableItem

// user's message bubble (right side)
class UserMessageItem(private val text: String) : BindableItem<ItemChatUserBinding>() {

    override fun getLayout() = R.layout.item_chat_user

    override fun bind(viewBinding: ItemChatUserBinding, position: Int) {
        viewBinding.tvMessage.text = text
    }

    override fun initializeViewBinding(view: View): ItemChatUserBinding {
        return ItemChatUserBinding.bind(view)
    }
}

// bot's response bubble (left side)
class BotMessageItem(private val text: String) : BindableItem<ItemChatBotBinding>() {

    override fun getLayout() = R.layout.item_chat_bot

    override fun bind(viewBinding: ItemChatBotBinding, position: Int) {
        viewBinding.tvMessage.text = text
    }

    override fun initializeViewBinding(view: View): ItemChatBotBinding {
        return ItemChatBotBinding.bind(view)
    }
}