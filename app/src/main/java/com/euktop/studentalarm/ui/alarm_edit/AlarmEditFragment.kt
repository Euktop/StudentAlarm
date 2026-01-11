package com.euktop.studentalarm.ui.alarm_edit

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.euktop.studentalarm.AlarmApplication
import com.euktop.studentalarm.ui.main.MainActivity
import com.euktop.studentalarm.R
import com.euktop.studentalarm.databinding.FragmentAlarmEditBinding
import com.euktop.studentalarm.utils.animation.AnimatorHelper
import com.euktop.studentalarm.utils.permission.PermissionManager
import com.euktop.studentalarm.viewmodel.AlarmEditViewModel
import com.euktop.studentalarm.viewmodel.UiMessage
import com.euktop.studentalarm.viewmodel.ViewModelFactory
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class AlarmEditFragment : Fragment() {
    private var _binding: FragmentAlarmEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlarmEditViewModel
    private var alarmId: Long = 0L

    // Флаги для предотвращения повторного открытия диалогов (временно оставляем)
    private var isTimePickerShowing = false
    private var isDaysDialogShowing = false
    private var isDescriptionDialogShowing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем alarmId из аргументов
        arguments?.let {
            alarmId = it.getLong("alarmId", 0L)
        }

        // Инициализируем ViewModel
        val app = requireActivity().application as AlarmApplication
        val factory = ViewModelFactory(app.alarmRepository, requireContext())
        viewModel = ViewModelProvider(this, factory)[AlarmEditViewModel::class.java]

        // Настраиваем UI
        setupUI()

        // Настраиваем наблюдателей
        setupObservers()

        // Загружаем данные будильника, если редактируем существующий
        if (alarmId > 0) {
            viewModel.loadAlarm(alarmId)
        }

        // Настраиваем обработчики кликов
        setupClickListeners()
    }

    private fun setupUI() {
        if (alarmId > 0) {
            binding.tvTitle.text = context?.getString(R.string.edit_alarm)
        } else {
            binding.tvTitle.text = context?.getString(R.string.new_alarm)
        }
    }

    // app/src/main/java/com/euktop/studentalarm/AlarmEditFragment.kt
// В методе setupObservers() исправляем обработку uiMessage:
    private fun setupObservers() {
        // Наблюдаем за состоянием будильника
        viewModel.alarmState.observe(viewLifecycleOwner) { state ->
            updateTimeDisplay(state.hour, state.minute)
            updateDaysDisplay(state.daysOfWeek)
            updateDescriptionDisplay(state.description)
        }

        // Наблюдаем за сообщениями UI
        viewModel.uiMessage.observe(viewLifecycleOwner) { message ->
            when (message) {
                is UiMessage.Success -> {
                    // Возвращаемся назад при успешном сохранении
                    findNavController().popBackStack()
                }
                is UiMessage.Error -> {
                    Toast.makeText(requireContext(), message.message, Toast.LENGTH_SHORT).show()
                }
                null -> {
                    // Игнорируем null значения
                }
            }
        }

        // Наблюдаем за показами диалогов
        viewModel.showTimePicker.observe(viewLifecycleOwner) { show ->
            if (show && !isTimePickerShowing) {
                showMaterialTimePicker()
                isTimePickerShowing = true
            }
        }

        viewModel.showDaysDialog.observe(viewLifecycleOwner) { show ->
            if (show && !isDaysDialogShowing) {
                showDaysDialog()
                isDaysDialogShowing = true
            }
        }

        viewModel.showDescriptionDialog.observe(viewLifecycleOwner) { show ->
            if (show && !isDescriptionDialogShowing) {
                showDescriptionDialog()
                isDescriptionDialogShowing = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMaterialTimePicker() {
        val state = viewModel.alarmState.value ?: return

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(state.hour)
            .setMinute(state.minute)
            .setTitleText(getString(R.string.alarm_time))
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setPositiveButtonText(getString(R.string.set))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.updateTime(picker.hour, picker.minute)
            viewModel.hideTimePicker()
            isTimePickerShowing = false
        }

        picker.addOnNegativeButtonClickListener {
            viewModel.hideTimePicker()
            isTimePickerShowing = false
        }

        picker.addOnDismissListener {
            viewModel.hideTimePicker()
            isTimePickerShowing = false
        }

        picker.show(childFragmentManager, "time_picker")
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimeDisplay(hour: Int, minute: Int) {
        binding.tvHour.text = String.format("%02d", hour)
        binding.tvMinute.text = String.format("%02d", minute)

        AnimatorHelper.bounceView(binding.tvHour, 1.2f, R.integer.anim_duration.toLong())
        AnimatorHelper.bounceView(binding.tvMinute, 1.2f, R.integer.anim_duration.toLong())
    }

    private fun showDaysDialog() {
        val state = viewModel.alarmState.value ?: return
        val tempSelectedDays = state.daysOfWeek.toMutableSet()

        val daysArray = arrayOf(
            getString(R.string.monday),
            getString(R.string.tuesday),
            getString(R.string.wednesday),
            getString(R.string.thursday),
            getString(R.string.friday),
            getString(R.string.saturday),
            getString(R.string.sunday)
        )

        val checkedItems = BooleanArray(7) { index ->
            tempSelectedDays.contains(index + 1)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_days))
            .setMultiChoiceItems(daysArray, checkedItems) { _, which, isChecked ->
                val dayNumber = which + 1
                if (isChecked) {
                    tempSelectedDays.add(dayNumber)
                } else {
                    tempSelectedDays.remove(dayNumber)
                }
            }
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                // Обновляем дни в ViewModel
                tempSelectedDays.forEach { day ->
                    // Устанавливаем все выбранные дни
                    // Для упрощения просто обновим весь набор
                }
                // Очищаем текущий набор и устанавливаем новый
                state.daysOfWeek.forEach { day ->
                    viewModel.toggleDayOfWeek(day) // Снимаем старые
                }
                tempSelectedDays.forEach { day ->
                    viewModel.toggleDayOfWeek(day) // Устанавливаем новые
                }
                viewModel.hideDaysDialog()
                isDaysDialogShowing = false
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                viewModel.hideDaysDialog()
                isDaysDialogShowing = false
            }
            .setOnDismissListener {
                viewModel.hideDaysDialog()
                isDaysDialogShowing = false
            }
            .show()
    }

    private fun updateDaysDisplay(daysOfWeek: Set<Int>) {
        val context = requireContext()
        val daysText = when {
            daysOfWeek.isEmpty() -> context.getString(R.string.once)
            daysOfWeek.size == 7 -> context.getString(R.string.daily)
            daysOfWeek.size == 5 && daysOfWeek.containsAll(setOf(1, 2, 3, 4, 5)) ->
                context.getString(R.string.weekdays)
            daysOfWeek.size == 2 && daysOfWeek.containsAll(setOf(6, 7)) ->
                context.getString(R.string.weekends)
            else -> daysOfWeek.sorted().joinToString(", ") { day ->
                when (day) {
                    1 -> context.getString(R.string.mon)
                    2 -> context.getString(R.string.tue)
                    3 -> context.getString(R.string.wed)
                    4 -> context.getString(R.string.thu)
                    5 -> context.getString(R.string.fri)
                    6 -> context.getString(R.string.sat)
                    7 -> context.getString(R.string.sun)
                    else -> ""
                }
            }
        }
        binding.tvSelectedDays.text = daysText
    }

    private fun showDescriptionDialog() {
        val state = viewModel.alarmState.value ?: return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_description, null)

        val editText = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val tvCharCount = dialogView.findViewById<TextView>(R.id.tvCharCount)
        val btnOk = dialogView.findViewById<Button>(R.id.btnDescriptionDialogOk)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDescriptionDialogCancel)

        editText.setText(state.description)

        val currentLength = state.description.length
        tvCharCount.text = "$currentLength"

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                tvCharCount.text = "$currentLength"
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.description))
            .setView(dialogView)
            .setOnDismissListener {
                viewModel.hideDescriptionDialog()
                isDescriptionDialogShowing = false
            }
            .create()

        btnOk.setOnClickListener {
            val newDescription = editText.text.toString().trim()
            viewModel.updateDescription(newDescription)
            viewModel.hideDescriptionDialog()
            isDescriptionDialogShowing = false
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            viewModel.hideDescriptionDialog()
            isDescriptionDialogShowing = false
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateDescriptionDisplay(description: String) {
        if (description.isNotEmpty()) {
            binding.tvDescription.text = description
        } else {
            binding.tvDescription.text = getString(R.string.alarms)
        }
    }

    private fun setupClickListeners() {
        // Выбор времени
        binding.timeSelectionContainer.setOnClickListener {
            viewModel.showTimePicker()
        }

        binding.tvHour.setOnClickListener {
            viewModel.showTimePicker()
        }

        binding.tvMinute.setOnClickListener {
            viewModel.showTimePicker()
        }

        // Выбор дней повторения
        binding.btnRepeat.setOnClickListener {
            viewModel.showDaysDialog()
        }

        // Описание
        binding.btnDescription.setOnClickListener {
            viewModel.showDescriptionDialog()
        }

        // Кнопки отмены и сохранения
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            checkPermissionsAndSave()
        }
    }

    private fun checkPermissionsAndSave() {
        // Проверяем валидность времени
        val state = viewModel.alarmState.value
        if (state != null && (state.hour < 0 || state.hour > 23 || state.minute < 0 || state.minute > 59)) {
            Toast.makeText(requireContext(), getString(R.string.invalid_time), Toast.LENGTH_SHORT).show()
            return
        }

        if (!PermissionManager.hasAllAlarmPermissions(requireContext())) {
            showPermissionsRequiredDialog()
            return
        }

        viewModel.saveAlarm()
    }

    private fun showPermissionsRequiredDialog() {
        val activity = requireActivity() as MainActivity
        activity.checkPermissionsAndExecute {
            Toast.makeText(
                requireContext(),
                getString(R.string.configure_permissions_and_try_again),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isTimePickerShowing = false
        isDaysDialogShowing = false
        isDescriptionDialogShowing = false
    }
}