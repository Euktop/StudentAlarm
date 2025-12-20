package com.euktop.studentalarm

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.euktop.studentalarm.databinding.FragmentAlarmEditBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmEditFragment : Fragment() {
    private var isTimePickerShowing = false
    private var isDaysDialogShowing = false
    private var isDescriptionDialogShowing = false

    val MAX_LENGHT_DESCRIPTION = 100
    private var _binding: FragmentAlarmEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlarmViewModel
    private var alarmId: Long = 0L
    private var selectedHour: Int = 12
    private var selectedMinute: Int = 30
    private var selectedDaysOfWeek: MutableList<Int> = mutableListOf()
    private var description: String = ""

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

        val app = requireActivity().application as AlarmApplication
        val viewModelFactory = ViewModelFactory(app.alarmRepository, requireContext())
        viewModel = ViewModelProvider(this, viewModelFactory)[AlarmViewModel::class.java]

        arguments?.let {
            alarmId = it.getLong("alarmId", 0L)
        }

        setupUI()
        loadAlarmData()
        setupClickListeners()
    }

    private fun setupUI() {
        if (alarmId > 0) {
            binding.tvTitle.text = context?.getString(R.string.EditingAlarm)
        } else {
            binding.tvTitle.text = context?.getString(R.string.NewAlarm)
        }

        updateTimeDisplay()
        updateDaysDisplay()
        updateDescriptionDisplay()

        binding.timeSelectionContainer.setOnClickListener {
            showMaterialTimePicker()
        }
    }

    private fun loadAlarmData() {
        if (alarmId > 0) {
            lifecycleScope.launch {
                val alarm = viewModel.getAlarmById(alarmId)
                alarm?.let {
                    selectedHour = it.hour
                    selectedMinute = it.minute
                    description = it.description
                    selectedDaysOfWeek = it.daysOfWeek.toMutableList()

                    updateTimeDisplay()
                    updateDaysDisplay()
                    updateDescriptionDisplay()
                } ?: run {
                    findNavController().popBackStack()
                }
            }
        } else {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 1)
            selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
            selectedMinute = calendar.get(Calendar.MINUTE)
            updateTimeDisplay()
        }
    }

    // ==================== MATERIAL TIME PICKER ====================
    @SuppressLint("SetTextI18n")
    private fun showMaterialTimePicker() {
        if (isTimePickerShowing) return

        isTimePickerShowing = true

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText(getString(R.string.SelectAlarmTime))
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setPositiveButtonText(getString(R.string.Set))
            .setNegativeButtonText(getString(R.string.Cancel))
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            updateTimeDisplay()
            isTimePickerShowing = false
        }

        picker.addOnNegativeButtonClickListener {
            isTimePickerShowing = false
        }

        picker.addOnDismissListener {
            isTimePickerShowing = false
        }

        picker.show(childFragmentManager, "time_picker")
    }

    private fun updateTimeDisplay() {
        binding.tvHour.text = String.format("%02d", selectedHour)
        binding.tvMinute.text = String.format("%02d", selectedMinute)

        // Анимация при изменении времени
        animateTimeChange()
    }

    private fun animateTimeChange() {
        AnimatorHelper.bounceView(binding.tvHour, 1.2f, R.integer.anim_duration.toLong())
        AnimatorHelper.bounceView(binding.tvMinute, 1.2f, R.integer.anim_duration.toLong())
    }

    // ==================== DAYS SELECTION DIALOG ====================
    private fun showDaysDialog() {
        if (isDaysDialogShowing) return

        isDaysDialogShowing = true

        val daysArray = arrayOf(
            getString(R.string.DayOfWeekMonday),
            getString(R.string.DayOfWeekTuesday),
            getString(R.string.DayOfWeekWednesday),
            getString(R.string.DayOfWeekThursday),
            getString(R.string.DayOfWeekFriday),
            getString(R.string.DayOfWeekSaturday),
            getString(R.string.DayOfWeekSunday)
        )

        // Создаем временную копию выбранных дней для работы внутри диалога
        val tempSelectedDays = selectedDaysOfWeek.toMutableList()

        val checkedItems = BooleanArray(7) { index ->
            tempSelectedDays.contains(index + 1)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.SelectDays))
            .setMultiChoiceItems(daysArray, checkedItems) { _, which, isChecked ->
                val dayNumber = which + 1
                if (isChecked) {
                    if (!tempSelectedDays.contains(dayNumber)) {
                        tempSelectedDays.add(dayNumber)
                    }
                } else {
                    tempSelectedDays.remove(dayNumber)
                }
            }
            .setPositiveButton(getString(R.string.OK)) { _, _ ->
                // Применяем изменения только при нажатии OK
                selectedDaysOfWeek.clear()
                selectedDaysOfWeek.addAll(tempSelectedDays)
                updateDaysDisplay()
                isDaysDialogShowing = false
            }
            .setNegativeButton(getString(R.string.Cancel)) { _, _ ->
                // При отмене ничего не делаем - tempSelectedDays игнорируется
                isDaysDialogShowing = false
            }
            .setOnDismissListener {
                isDaysDialogShowing = false
            }
            .show()
    }

    private fun updateDaysDisplay() {
        val context = requireContext()
        val daysText = when {
            selectedDaysOfWeek.isEmpty() -> context.getString(R.string.RepeatOnce)
            selectedDaysOfWeek.size == 7 -> context.getString(R.string.RepeatDaily)
            selectedDaysOfWeek.size == 5 && selectedDaysOfWeek.containsAll(listOf(1, 2, 3, 4, 5)) ->
                context.getString(R.string.RepeatWeekdays)
            selectedDaysOfWeek.size == 2 && selectedDaysOfWeek.containsAll(listOf(6, 7)) ->
                context.getString(R.string.RepeatWeekends)
            else -> selectedDaysOfWeek.sorted().joinToString(", ") { day ->
                when (day) {
                    1 -> context.getString(R.string.DayOfWeekMondayShort)
                    2 -> context.getString(R.string.DayOfWeekTuesdayShort)
                    3 -> context.getString(R.string.DayOfWeekWednesdayShort)
                    4 -> context.getString(R.string.DayOfWeekThursdayShort)
                    5 -> context.getString(R.string.DayOfWeekFridayShort)
                    6 -> context.getString(R.string.DayOfWeekSaturdayShort)
                    7 -> context.getString(R.string.DayOfWeekSundayShort)
                    else -> ""
                }
            }
        }
        binding.tvSelectedDays.text = daysText
    }

    // ==================== DESCRIPTION DIALOG ====================
    private fun showDescriptionDialog() {
        if (isDescriptionDialogShowing) return

        isDescriptionDialogShowing = true

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_description, null)

        val editText = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val tvCharCount = dialogView.findViewById<TextView>(R.id.tvCharCount)
        val btnOk = dialogView.findViewById<Button>(R.id.btnDescriptionDialogOk)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDescriptionDialogCancel)

        editText.setText(description)
        updateCharCount(editText, tvCharCount)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharCount(editText, tvCharCount)

                val currentText = s?.toString() ?: ""
                if (currentText.length > MAX_LENGHT_DESCRIPTION) {
                    editText.setText(currentText.substring(0, MAX_LENGHT_DESCRIPTION))
                    editText.setSelection(MAX_LENGHT_DESCRIPTION)

                    if (count > 0) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.MaximumDescriptionLength).replace("{x}",MAX_LENGHT_DESCRIPTION.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.DescriptionAlarm))
            .setView(dialogView)
            .setOnDismissListener {
                isDescriptionDialogShowing = false
            }
            .create()

        btnOk.setOnClickListener {
            description = editText.text.toString().trim()
            updateDescriptionDisplay()
            isDescriptionDialogShowing = false
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            isDescriptionDialogShowing = false
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateCharCount(editText: EditText, tvCharCount: TextView) {
        val currentLength = editText.text.length
        tvCharCount.text = "$currentLength/$MAX_LENGHT_DESCRIPTION"
    }

    private fun updateDescriptionDisplay() {
        if (description.isNotEmpty()) {
            binding.btnDescription.text = getString(R.string.ChangeDescription)
            binding.tvDescription.text = description
        } else {
            binding.btnDescription.text = getString(R.string.AddDescription)
            binding.tvDescription.text = getString(R.string.Alarm)
        }
    }

    // ==================== CLICK LISTENERS ====================
    private fun setupClickListeners() {
        binding.tvHour.setOnClickListener { showMaterialTimePicker() }
        binding.tvMinute.setOnClickListener { showMaterialTimePicker() }

        binding.btnRepeat.setOnClickListener { showDaysDialog() }

        binding.btnDescription.setOnClickListener { showDescriptionDialog() }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            checkPermissionsAndSave()
        }
    }

    // ==================== CHECK PERMISSIONS AND SAVE ====================
    private fun checkPermissionsAndSave() {
        if (!validateAlarm()) {
            return
        }

        // Проверяем наличие всех необходимых разрешений
        if (!PermissionManager.hasAllAlarmPermissions(requireContext())) {
            showPermissionsRequiredDialog()
            return
        }

        // Все разрешения есть, сохраняем будильник
        saveAlarm()
    }

    private fun showPermissionsRequiredDialog() {
        val activity = requireActivity() as MainActivity
        activity.checkAlarmPermissionsAndExecute {
            // Пользователь перешел в настройки, но мы все равно не сохраняем будильник
            // Он может создать будильник после возвращения и настройки разрешений
            Toast.makeText(
                requireContext(),
                "Настройте разрешения и попробуйте снова",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveAlarm() {
        val alarm = Alarm(
            id = alarmId,
            hour = selectedHour,
            minute = selectedMinute,
            description = description,
            daysOfWeek = selectedDaysOfWeek,
            isEnabled = true
        )

        lifecycleScope.launch {
            try {
                if (alarmId > 0) {
                    viewModel.updateAlarm(alarm)
                } else {
                    viewModel.insertAlarm(alarm)
                }
                findNavController().popBackStack()
            } catch (e: Exception) {
                // Не показываем сообщение об ошибке
            }
        }
    }

    private fun validateAlarm(): Boolean {
        if (selectedHour < 0 || selectedHour > 23 || selectedMinute < 0 || selectedMinute > 59) {
            Toast.makeText(requireContext(), getString(R.string.IncorrectTime), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()

        isTimePickerShowing = false
        isDaysDialogShowing = false
        isDescriptionDialogShowing = false

        _binding = null
    }
}