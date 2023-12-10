import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import javax.swing.table.DefaultTableModel;

public class TodoListApp extends JFrame {
    
    private JList<Task> toDoList;
    private JPanel calendarPanel;
    private JTable calendarTable;
    private JComboBox<String> monthComboBox;
    private JComboBox<String> yearComboBox;
    
    private DefaultListModel<Task> toDoListModel = new DefaultListModel<>();
    private DefaultTableModel calendarModel;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final List<Task> originalTasks = new ArrayList<>();;
    private Date selectedDate = null;
    private final String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    
    public static void main(String[] args) {
        setLookAndFeel("Nimbus");
        SwingUtilities.invokeLater(() -> new TodoListApp());
    }
    
    public TodoListApp() {
        initComponents();
        loadTasksFromFile();
        setVisible(true);
    }

    private void initComponents() {
        // Initialize JFrame
        setTitle("To-Do List");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Add calendar panel
        initializeCalendarPanel();
        add(calendarPanel, BorderLayout.NORTH);
        
        // Add control panel
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton filterCompletedButton = new JButton("Completed");
        JButton filterIncompleteButton = new JButton("Incomplete");
        JButton showAllButton = new JButton("Show All");
                
        JPanel controlPanel = new JPanel();
        controlPanel.add(addButton);
        controlPanel.add(removeButton);
        controlPanel.add(filterCompletedButton);
        controlPanel.add(filterIncompleteButton);
        controlPanel.add(showAllButton);
        
        add(controlPanel, BorderLayout.SOUTH);
        
        // Control panel functionality
        addButton.addActionListener((var e) -> showAddTaskMenu());
        removeButton.addActionListener((var e) -> removeSelectedTask());
        filterCompletedButton.addActionListener((var e) -> filterTasks(new FilterOptions(true, false)));
        filterIncompleteButton.addActionListener((var e) -> filterTasks(new FilterOptions(false, true)));
        showAllButton.addActionListener((var e) -> filterTasks(new FilterOptions()));
        
        // Set up todo list
        toDoList = new JList<>(toDoListModel);
        toDoList.setCellRenderer(new CheckboxListCellRenderer());
        
        toDoList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = toDoList.locationToIndex(e.getPoint());
                if (index == -1) return;
                
                Task task = toDoListModel.get(index);
                task.setCompleted(!task.isCompleted());
                toDoList.repaint();
            }
        });
        
        add(new JScrollPane(toDoList), BorderLayout.CENTER);
    }
    
    private class AddTaskMenu extends JPanel {
        public JTextField descriptionField;
        public JTextField dueDateField;
        
        public AddTaskMenu() {
            initComponents();
        }
        
        private void initComponents() {
            setLayout(new GridLayout(3, 2));
            descriptionField = new JTextField();
            dueDateField = new JTextField();
            
            add(new JLabel("Description:"));
            add(descriptionField);
            add(new JLabel("Due Date (YYYY-MM-DD):"));
            add(dueDateField);
        }
    }
    
    private void showAddTaskMenu() {
        // Create a custom dialog for adding a task
        AddTaskMenu dialogPanel = new AddTaskMenu();

        int result = JOptionPane.showConfirmDialog(
            TodoListApp.this,
            dialogPanel,
            "Add a Task",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String taskDescription = dialogPanel.descriptionField.getText();
            String dueDateString = dialogPanel.dueDateField.getText();

            if (!taskDescription.isEmpty() && !dueDateString.isEmpty()) {
                try {
                    Date dueDate = dateFormat.parse(dueDateString);
                    Task task = new Task(taskDescription, false, dueDate);
                    toDoListModel.addElement(task);
                    originalTasks.add(task); // Add the task to originalTasks
                    saveTasksToFile(); // Save tasks after adding
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(
                        TodoListApp.this,
                        "Invalid due date format. Please use YYYY-MM-DD.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
    }
    
    private void removeSelectedTask() {
        int selectedIndex = toDoList.getSelectedIndex();
        if (selectedIndex != -1) {
            Task removedTask = toDoListModel.getElementAt(selectedIndex); // Get the selected task
            toDoListModel.remove(selectedIndex); // Remove from the list model
            originalTasks.remove(removedTask); // Remove from the originalTasks ArrayList
            saveTasksToFile(); // Save tasks after removing
        }
    }
    
    private class FilterOptions {
        public boolean showComplete = true;
        public boolean showIncomplete = true;
        
        public FilterOptions() { }
        public FilterOptions(boolean showComplete, boolean showIncomplete) {
            this.showComplete = showComplete;
            this.showIncomplete = showIncomplete;
        }
    }
    
    private void filterTasks(FilterOptions options) {
        toDoListModel.clear();
        for (Task task : originalTasks) {
            if (options.showComplete && task.isCompleted()) toDoListModel.addElement(task);
            else if (options.showIncomplete && !task.isCompleted()) toDoListModel.addElement(task);
        }
    }
    
    private void initializeCalendarPanel() {
        calendarPanel = new JPanel(new BorderLayout());

        // Month and Year Selection
        
        monthComboBox = new JComboBox<>(months);
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH); // Calendar.MONTH is zero-based
        monthComboBox.setSelectedIndex(currentMonth);
        monthComboBox.addActionListener(e -> updateCalendar());

        // Year selection from 2023 to a future year
        yearComboBox = new JComboBox<>();
        for (int year = 2023; year <= 2030; year++) {
            yearComboBox.addItem(String.valueOf(year));
        }
        yearComboBox.addActionListener(e -> updateCalendar());

        JPanel selectionPanel = new JPanel();
        selectionPanel.add(monthComboBox);
        selectionPanel.add(yearComboBox);  // Added year selection to the panel
        calendarPanel.add(selectionPanel, BorderLayout.NORTH);

        // Calendar Display
        calendarTable = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make cells non-editable
            }
        };

        // Add mouse listener to handle date selection
        calendarTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = calendarTable.rowAtPoint(evt.getPoint());
                int col = calendarTable.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    Object selectedValue = calendarTable.getValueAt(row, col);
                    if (selectedValue != null) {
                        int day = Integer.parseInt(selectedValue.toString());
                        int month = monthComboBox.getSelectedIndex(); // 0-based month
                        int year = Integer.parseInt(yearComboBox.getSelectedItem().toString());

                        // Update the selectedDate
                        Calendar cal = Calendar.getInstance();
                        cal.set(year, month, day);
                        selectedDate = cal.getTime();

                        // Fetch and display tasks for the selected date
                        filterByDate(selectedDate);
                    }
                }
            }
        });

        calendarTable.setRowHeight(50);
        Dimension preferredSize = new Dimension(7 * 50, 6 * 50);
        calendarTable.setPreferredScrollableViewportSize(preferredSize);
        calendarTable.setDefaultEditor(Object.class, null); // Make cells non-editable
        calendarModel = new DefaultTableModel(new Object[][]{}, new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"});
        calendarTable.setModel(calendarModel);
        calendarPanel.add(new JScrollPane(calendarTable), BorderLayout.CENTER);

        // Initialize calendar display
        updateCalendar();
    }
    
    private void updateCalendar() {
        // Get the selected month and year
        int month = monthComboBox.getSelectedIndex() + 1;  // Months are 0-based
        int year = Integer.parseInt(yearComboBox.getSelectedItem().toString());

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfMonth = cal.get(Calendar.DAY_OF_WEEK);  // 1st day of the month

        // Get the number of days in the month
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendarModel.setRowCount(0);
        Object[][] calendarData = new Object[6][7];

        // Fill in the calendar data with the appropriate dates
        int day = 1;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                if ((i == 0 && j < firstDayOfMonth - 1) || (day > daysInMonth)) {
                    calendarData[i][j] = null;
                } else {
                    calendarData[i][j] = day;
                    day++;
                }
            }
        }

        for (int i = 0; i < 6; i++) {
            calendarModel.addRow(calendarData[i]);
        }
    }

    private void saveTasksToFile() {
        try (PrintWriter writer = new PrintWriter("tasks.txt")) {
            for (int i = 0; i < toDoListModel.size(); i++) {
                Task task = toDoListModel.getElementAt(i);
                String taskText = task.getText();
                boolean completed = task.isCompleted(); // Get the completion status
                Date dueDate = task.getDueDate();
                String dueDateString = dateFormat.format(dueDate);
                writer.println(taskText + "|" + completed + "|" + dueDateString); // Include completion status
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTasksFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("tasks.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    String taskText = parts[0];
                    boolean completed = Boolean.parseBoolean(parts[1]);
                    Date dueDate = dateFormat.parse(parts[2]);
                    Task task = new Task(taskText, completed, dueDate);
                    toDoListModel.addElement(task);
                    originalTasks.add(task);
                }
            }
	    toDoList.repaint();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void showAllTasks() {
        toDoListModel.clear();
        for (Task task : originalTasks) {
            toDoListModel.addElement(task);
        }
    }
    
    private void filterByDate(Date selectedDate) {
        toDoListModel.clear();
        for (Task task : originalTasks) {
            if (isSameDay(task.getDueDate(), selectedDate)) {
                toDoListModel.addElement(task);
            }
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
            && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
            && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
    
    private static void setLookAndFeel(String name) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if (name.equals(info.getName())) {
                    System.out.println(info.getClassName());
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TodoListApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    class CheckboxListCellRenderer extends JPanel implements ListCellRenderer<Task> {
        private final JCheckBox checkBox;
        private final JLabel label;
        private final JLabel dueDateLabel;

        public CheckboxListCellRenderer() {
            setLayout(new BorderLayout());
            
            checkBox = new JCheckBox();
            label = new JLabel();
            dueDateLabel = new JLabel();
            add(checkBox, BorderLayout.WEST);
            add(label, BorderLayout.CENTER);
            add(dueDateLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Task> list, Task value, int index, boolean isSelected, boolean cellHasFocus) {
            checkBox.setSelected(value.isCompleted());
            checkBox.setEnabled(list.isEnabled());

            label.setText(value.getText());

            Date dueDate = value.getDueDate();
            dueDateLabel.setText(dateFormat.format(dueDate));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    class Task {
        public final String text;
        private boolean completed;
        private final Date dueDate;

        public Task(String text, boolean completed, Date dueDate) {
            this.text = text;
            this.completed = completed;
            this.dueDate = dueDate;
        }

        public String getText() { return text; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public Date getDueDate() { return dueDate; }
    }
}
