package falkeadler.application.exoplayertest.videoplayer.player

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.OptionItemBinding
import falkeadler.application.exoplayertest.videoplayer.databinding.OptionSelectLayoutBinding

class OptionSelectBottomSheet(private val title: String,
                              private val itemList: List<String>,
                              private val callback: (Int) -> Unit): BottomSheetDialogFragment() {
    companion object {
        const val TAG = "OptionSelectBottomSheet"
    }
    private var binding: OptionSelectLayoutBinding? = null
    private var lastSelected = -1
    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = OptionSelectLayoutBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.let {
            it.closeBtn.setOnClickListener { dismiss() }
            it.headerText.text = title
            it.optionList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            it.optionList.adapter = OptionItemAdapter() {
                selection ->
                lastSelected = selection
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (lastSelected != -1 && itemList.isNotEmpty()) {
            callback(lastSelected)
        }
    }

    private inner class OptionItemAdapter(val cb : (Int)-> Unit): RecyclerView.Adapter<OptionItemAdapter.OptionHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionHolder {
            return OptionHolder(OptionItemBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
        }

        override fun getItemCount(): Int = itemList.size

        override fun onBindViewHolder(holder: OptionHolder, position: Int) {
            holder.configure(itemList[position])
        }

        inner class OptionHolder(val binding: OptionItemBinding): RecyclerView.ViewHolder(binding.root) {
            fun configure(item: String) {
                binding.optionItem.text = item
                binding.root.setOnClickListener {
                    cb(absoluteAdapterPosition)
                }
            }
        }
    }
}