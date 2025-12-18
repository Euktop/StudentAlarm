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
import androidx.navigation.NavOptions
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
    }
    private var isFabHidden = false
    private var isAnimating = false

    private fun setupScrollListenerWithTranslation() {
        val fabHideThreshold = 10
        var isAtTop: Boolean

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var accumulatedDy = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val canScrollUp = recyclerView.canScrollVertically(-1)
                isAtTop = !canScrollUp

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

        AnimatorHelper.slideView(
            view = binding.fabAddAlarm,
            fromY = 0f,
            toY = translationY,
            duration = resources.getInteger(R.integer.anim_duration_short).toLong(),
            onStart = { isAnimating = true },
            onEnd = {
                isAnimating = false
                isFabHidden = true
            }
        )
    }

    private fun showFab() {
        isAnimating = true
        AnimatorHelper.slideView(
            view = binding.fabAddAlarm,
            fromY = binding.fabAddAlarm.translationY,
            toY = 0f,
            duration = resources.getInteger(R.integer.anim_duration_short).toLong(),
            onStart = { isAnimating = true },
            onEnd = {
                isAnimating = false
                isFabHidden = false
            }
        )
    }

    override fun onResume() {
        super.onResume()
        binding.fabAddAlarm.translationY = 0f
        isFabHidden = false
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
                    .setExitAnim(R.anim.fade_out)
                    .setPopEnterAnim(R.anim.fade_in)
                    .setPopExitAnim(R.anim.slide_out_bottom)
                    .build()
            )
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
                    .setExitAnim(R.anim.fade_out)
                    .setPopEnterAnim(R.anim.fade_in)
                    .setPopExitAnim(R.anim.slide_out_bottom)
                    .build()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}