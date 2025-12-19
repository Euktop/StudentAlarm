package com.euktop.studentalarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.euktop.studentalarm.databinding.FragmentAlarmsBinding
import kotlinx.coroutines.launch
import kotlin.math.abs

class AlarmsFragment : Fragment() {

    private var _binding: FragmentAlarmsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AlarmRecyclerAdapter
    private lateinit var viewModel: AlarmViewModel

    private var isFabHidden = false
    private var isAnimating = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as AlarmApplication
        val viewModelFactory = ViewModelFactory(app.alarmRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[AlarmViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupScrollListenerWithTranslation()
        setupSelectionListeners()
    }

    private fun setupSelectionListeners() {
        adapter.onSelectionModeChanged = { isSelectionMode ->
            if (isSelectionMode) {
                showSelectionMode()
            } else {
                hideSelectionMode()
            }
        }

        adapter.onSelectedCountChanged = { count ->
            binding.tvSelectedCount.text = "Выделено: $count"

            // Меняем иконку кнопки выделить все
            if (adapter.isAllSelected()) {
                binding.btnToggleSelectAll.setImageResource(R.drawable.deselect_all)
                binding.btnToggleSelectAll.contentDescription = "Снять выделение"
            } else {
                binding.btnToggleSelectAll.setImageResource(R.drawable.select_all)
                binding.btnToggleSelectAll.contentDescription = "Выделить все"
            }
        }
    }

    private fun setupScrollListenerWithTranslation() {
        val fabHideThreshold = 10
        var isAtTop = true

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var accumulatedDy = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val canScrollUp = recyclerView.canScrollVertically(-1)
                isAtTop = !canScrollUp

                // Не скрываем FAB в режиме выделения
                if (adapter.isSelectionMode()) {
                    return
                }

                if (isAtTop && isFabHidden && !isAnimating) {
                    showFab()
                    accumulatedDy = 0
                    return
                }

                accumulatedDy += dy

                if (abs(accumulatedDy) > fabHideThreshold) {
                    if (accumulatedDy > 0 && !isFabHidden && !isAnimating) {
                        hideFab()
                    }
                    else if (accumulatedDy < 0 && isFabHidden && !isAnimating) {
                        showFab()
                    }
                    accumulatedDy = 0
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        val canScrollUp = recyclerView.canScrollVertically(-1)
                        isAtTop = !canScrollUp
                        if (isAtTop && isFabHidden && !isAnimating) {
                            showFab()
                        }
                        accumulatedDy = 0
                    }
                }
            }
        })
    }

    private fun hideFab() {
        isAnimating = true
        val layoutParams = binding.fabAddAlarm.layoutParams as FrameLayout.LayoutParams
        val translationY = (binding.fabAddAlarm.height + layoutParams.bottomMargin).toFloat()

        binding.fabAddAlarm.animate()
            .translationY(translationY)
            .setDuration(resources.getInteger(R.integer.anim_duration_short).toLong())
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                isAnimating = false
                isFabHidden = true
            }
            .start()
    }

    private fun showFab() {
        isAnimating = true
        binding.fabAddAlarm.animate()
            .translationY(0f)
            .setDuration(resources.getInteger(R.integer.anim_duration_short).toLong())
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isAnimating = false
                isFabHidden = false
            }
            .start()
    }
    private fun setupRecyclerView() {
        adapter = AlarmRecyclerAdapter(requireContext())

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        adapter.onItemClick = { alarm ->
            val bundle = Bundle().apply {
                putLong("alarmId", alarm.id)
            }
            findNavController().navigate(
                R.id.action_alarmsFragment_to_alarmEditFragment,
                bundle,
                androidx.navigation.NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_bottom)
                    .setExitAnim(android.R.anim.fade_out)
                    .setPopEnterAnim(android.R.anim.fade_in)
                    .setPopExitAnim(R.anim.slide_out_bottom)
                    .build()
            )
        }

        adapter.onItemLongClick = { alarm ->
            // Режим выделения активируется автоматически в адаптере
        }

        adapter.onSwitchChanged = { alarm, isChecked ->
            lifecycleScope.launch {
                viewModel.updateAlarm(alarm.copy(isEnabled = isChecked))
            }
        }

        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allAlarms.observe(viewLifecycleOwner) { alarms ->
            adapter.updateAlarms(alarms)
            binding.emptyStateTextView.visibility =
                if (alarms.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.fabAddAlarm.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("alarmId", 0L)
            }
            findNavController().navigate(
                R.id.action_alarmsFragment_to_alarmEditFragment,
                bundle,
                androidx.navigation.NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_bottom)
                    .setExitAnim(android.R.anim.fade_out)
                    .setPopEnterAnim(android.R.anim.fade_in)
                    .setPopExitAnim(R.anim.slide_out_bottom)
                    .build()
            )
        }

        // Кнопка выхода из режима выделения
        binding.btnCloseSelection.setOnClickListener {
            adapter.exitSelectionMode()
        }

        // Кнопка выделить все/снять выделение
        binding.btnToggleSelectAll.setOnClickListener {
            if (adapter.isAllSelected()) {
                adapter.deselectAll()
            } else {
                adapter.selectAll()
            }
        }

        // Кнопка удаления выделенных будильников
        binding.fabDelete.setOnClickListener {
            deleteSelectedAlarms()
        }
    }

    private fun showSelectionMode() {
         binding.selectionToolbar.visibility = View.VISIBLE
        binding.fabDelete.visibility = View.VISIBLE
        binding.fabAddAlarm.visibility = View.GONE

        binding.selectionToolbar.post {
            val toolbarHeight = binding.selectionToolbar.height

            binding.recyclerView.setPadding(
                binding.recyclerView.paddingLeft,
                toolbarHeight,
                binding.recyclerView.paddingRight,
                binding.recyclerView.paddingBottom
            )
        }
    }

    private fun hideSelectionMode() {
        binding.selectionToolbar.visibility = View.GONE
        binding.fabDelete.visibility = View.GONE
        binding.fabAddAlarm.visibility = View.VISIBLE

        binding.recyclerView.setPadding(
            binding.recyclerView.paddingLeft,
            0,
            binding.recyclerView.paddingRight,
            binding.recyclerView.paddingBottom
        )

        binding.fabAddAlarm.translationY = 0f
        isFabHidden = false
    }

    private fun deleteSelectedAlarms() {
        val selectedAlarms = adapter.getSelectedAlarms()
        if (selectedAlarms.isEmpty()) return

        lifecycleScope.launch {
            selectedAlarms.forEach { alarm ->
                viewModel.deleteAlarm(alarm)
            }
            adapter.exitSelectionMode()
        }
    }

    // Обработка кнопки "Назад"
    override fun onResume() {
        super.onResume()
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                if (adapter.isSelectionMode()) {
                    adapter.exitSelectionMode()
                    return@setOnKeyListener true
                }
            }
            false
        }
        binding.fabAddAlarm.translationY = 0f
        isFabHidden = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}