import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import java.util.Collections;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class TodoListApp extends JFrame {
    
    private JList<Task> toDoList;
    private JPanel calendarPanel;
    private JTable calendarTable;
    private JComboBox<String> monthComboBox;
    private JSpinner yearSpinner;
    
    private DefaultListModel<Task> toDoListModel = new DefaultListModel<>();
    private DefaultTableModel calendarModel;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat prettyDateFormat = new SimpleDateFormat("MMM d, yyyy");
    private final List<Task> originalTasks = new ArrayList<>();;
    private Date selectedDate = null;
    private final String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    String[] priorityValues = {"Low", "Medium", "High"};
    
    public static void main(String[] args) {
        setLookAndFeel("Nimbus");
        SwingUtilities.invokeLater(() -> new TodoListApp());
    }
    
    private static void setLookAndFeel(String name) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if (name.equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TodoListApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    public TodoListApp() {
        initComponents();
        loadTasksFromFile();
        setVisible(true);
    }

    private void initComponents() {
        // Initialize JFrame
        setTitle("To-Do List");
        setSize(460, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Add calendar panel
        calendarPanel = new CalendarPanel();
        add(calendarPanel, BorderLayout.NORTH);
        
        // Add control panel (bottom buttons)
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton filterCompletedButton = new JButton("Completed");
        JButton filterIncompleteButton = new JButton("Incomplete");
        JButton showAllButton = new JButton("Show All");
    
        JButton filterLowPriorityButton = new JButton("Low Priority");
        JButton filterMediumPriorityButton = new JButton("Med Priority");
        JButton filterHighPriorityButton = new JButton("High Priority");
    
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.add(addButton);
        controlPanel.add(removeButton);
        controlPanel.add(filterCompletedButton);
        controlPanel.add(filterIncompleteButton);
        controlPanel.add(showAllButton);
        
        JPanel priorityPanel = new JPanel();
        priorityPanel.setLayout(new BoxLayout(priorityPanel, BoxLayout.X_AXIS));
        priorityPanel.add(filterLowPriorityButton);
        priorityPanel.add(filterMediumPriorityButton);
        priorityPanel.add(filterHighPriorityButton);

        JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));
        masterPanel.add(controlPanel);
        masterPanel.add(priorityPanel);
        
        // Ensure there is a gap between bottom of master panel and bottom of UI
        masterPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                masterPanel.setPreferredSize(new Dimension(masterPanel.getWidth(), controlPanel.getHeight() + priorityPanel.getHeight() + 4));
            }
        });

        add(masterPanel, BorderLayout.SOUTH);
    
        // Control panel functionality
        addButton.addActionListener(e -> showAddTaskMenu());
        removeButton.addActionListener(e -> removeSelectedTask());
        filterCompletedButton.addActionListener(e -> filterTasks(new FilterOptions(true, false)));
        filterIncompleteButton.addActionListener(e -> filterTasks(new FilterOptions(false, true)));
        showAllButton.addActionListener(e -> filterTasks(new FilterOptions()));
    
        filterLowPriorityButton.addActionListener(e -> filterTasks(new FilterOptions(null, "low")));
        filterMediumPriorityButton.addActionListener(e -> filterTasks(new FilterOptions(null, "medium")));
        filterHighPriorityButton.addActionListener(e -> filterTasks(new FilterOptions(null, "high")));
        
        // Set up todo list
        toDoList = new JList<>(toDoListModel);
        toDoList.setCellRenderer(new CheckboxListCellRenderer());
        
        toDoList.addMouseListener(new MouseAdapter() {
            @Override 
            public void mousePressed(MouseEvent e) {
                int index = toDoList.locationToIndex(e.getPoint());
                if (index == -1) return;
                
                Task task = toDoListModel.get(index);
                // Toggle selected only if checkbox area clicked
                if (e.getPoint().x <= 32) {
                    task.setCompleted(!task.isCompleted());
                    sortTasks();
                    tasksChangedUpdate();
                }
                
                toDoList.repaint();
            }
        });
        
        add(new JScrollPane(toDoList), BorderLayout.CENTER);
    }
    
    // The calender element and its functionality
    private class CalendarPanel extends JPanel {
        public CalendarPanel() {
            initComponents();
        }
        
        private void initComponents() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 

            // Build top selection panel
            Calendar calendar = Calendar.getInstance();
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
            monthComboBox = new JComboBox<>(months);
            monthComboBox.setSelectedIndex(currentMonth);
            monthComboBox.addActionListener(e -> updateCalendar());
            
            SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear , currentYear - 100, currentYear + 100, 1);
            yearSpinner = new JSpinner(yearModel);
            yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
            yearSpinner.addChangeListener(e -> updateCalendar());

            JButton todayButton = new JButton("Today");
            todayButton.addActionListener(e -> {
                yearSpinner.setValue(currentYear);
                monthComboBox.setSelectedIndex(currentMonth);
            });

            JPanel dateSelectionPanel = new JPanel();
            dateSelectionPanel.add(monthComboBox);
            dateSelectionPanel.add(yearSpinner);  // Added year selection to the panel
            dateSelectionPanel.add(todayButton);
            add(dateSelectionPanel);

            // Build calendar
            calendarTable = new JTable() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; 
                }
            };

            // On calendar click, set selectedDate & filter tasks 
            calendarTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent evt) {
                    int row = calendarTable.rowAtPoint(evt.getPoint());
                    int col = calendarTable.columnAtPoint(evt.getPoint());
                    if (row >= 0 && col >= 0) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.MONTH, monthComboBox.getSelectedIndex());
                        cal.set(Calendar.YEAR, (Integer) yearSpinner.getValue());
                        cal.set(Calendar.DAY_OF_MONTH, 1);

                        int firstDayOfMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;  // 1st day of the month
                        int day = 1 + (row * 7) + col - firstDayOfMonth;

                        cal.set(Calendar.DATE, day);
                        selectedDate = cal.getTime();

                        filterTasks(new FilterOptions(selectedDate));
                    }
                }
            });

            calendarTable.setRowHeight(65);
            calendarTable.setDefaultEditor(Object.class, null); // Make cells non-editable
            
            // Build calendar data model
            calendarModel = new DefaultTableModel(new Object[][]{}, new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"});
            calendarModel.setRowCount(6);
            calendarTable.setModel(calendarModel);
            DayRenderer customRenderer = new DayRenderer();
            for (int i = 0; i < calendarTable.getColumnCount(); i++) {
                calendarTable.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
            }     

            // ScrollPane wrapper for calendar
            JScrollPane parentPanel = new JScrollPane(calendarTable);
            
            // Fit parent scroll pane to calendarTable
            int totalRowHeight = (calendarTable.getRowHeight() + 1) * calendarTable.getRowCount();
            if (calendarTable.getTableHeader() != null) totalRowHeight += calendarTable.getTableHeader().getPreferredSize().height;
            parentPanel.setPreferredSize(new Dimension(calendarTable.getColumnModel().getTotalColumnWidth(), totalRowHeight));

            add(parentPanel);
        }
    }
    
    // Should be called whenever the calendar needs to be updated/changed
    private void updateCalendar() {
        calendarModel.fireTableDataChanged();
    }
    
    //  Renderer for each cell (day) in calendary
    private class DayRenderer extends JPanel implements TableCellRenderer {
        JLabel dayLabel;
        JLabel notifLabel;

        public DayRenderer() {
            initComponents();
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());

            // Large main day label
            dayLabel = new JLabel();
            dayLabel.setFont(new Font(dayLabel.getFont().getName(), Font.BOLD, 20));
            dayLabel.setPreferredSize(new Dimension(32, 0));
            dayLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(dayLabel, BorderLayout.WEST);

            // Small notification label
            notifLabel = new JLabel();
            notifLabel.setHorizontalAlignment(SwingConstants.LEFT);
            notifLabel.setVerticalAlignment(SwingConstants.BOTTOM);
            notifLabel.setPreferredSize(new Dimension(12, 0));
            notifLabel.setFont(new Font(notifLabel.getFont().getName(), Font.BOLD, 12));
            notifLabel.setForeground(new Color(150,0,0));
            add(notifLabel, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {         
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            // Set background on border based on selected, or even/odd row
            if (isSelected && table.getSelectedRow() == row && table.getSelectedColumn() == column) {
                setBackground(new Color(210,240,220));
                setBorder(BorderFactory.createLineBorder(new Color(80, 200, 120)));
            } else if (row % 2 == 0) {
                setBackground(Color.WHITE);
            } else {
                setBackground(new Color(242,242,242));
            }

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, monthComboBox.getSelectedIndex());
            cal.set(Calendar.YEAR, (Integer) yearSpinner.getValue());
            cal.set(Calendar.DAY_OF_MONTH, 1);

            int firstDayOfMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;  // 1st day of the month
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            // Calculate day based on row and column
            int day = 1 + (row * 7) + column - firstDayOfMonth;
            cal.set(Calendar.DAY_OF_MONTH, day);
            
            dayLabel.setText((day > 0 && day <= daysInMonth) ? ("" + day) : "");
            
            // Get tasks on this day for notif label
            int matchingTasks = 0;
            for (Task task : originalTasks) {
                if (isSameDay(task.getDueDate(), cal.getTime())) 
                    matchingTasks++;
            }
            
            notifLabel.setText(matchingTasks > 0 ? ("" + matchingTasks) : "");
            return this;
        }
    }
    
    // Data for each task
    class Task {
        public final String text;
        private boolean completed;
        private final Date dueDate;
        private String priority;

        public Task(String text, boolean completed, Date dueDate, String priority) {
            this.text = text;
            this.completed = completed;
            this.dueDate = dueDate;
            this.priority = priority;
        }

        public String getText() { return text; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public Date getDueDate() { return dueDate; }
        public String getPriority() { return priority; }
    }

    // Renderer for each task in the list
    class CheckboxListCellRenderer extends JPanel implements ListCellRenderer<Task> {
        private final JLabel label;
        private final JCheckBox checkBox;
        private final JLabel dueDateLabel;
        private final JComboBox<String> priorityField;

        public CheckboxListCellRenderer() {
            setLayout(new BorderLayout());

            label = new JLabel();
            label.setPreferredSize(new Dimension(16, 28));
            label.setFont(new Font(label.getFont().getName(), label.getFont().getStyle(), 16));
            add(label, BorderLayout.CENTER);

            priorityField = new JComboBox<>(priorityValues);
            add(priorityField, BorderLayout.EAST);
            
            dueDateLabel = new JLabel();
            dueDateLabel.setPreferredSize(new Dimension(90, 28));
            dueDateLabel.setHorizontalAlignment(SwingConstants.LEFT);
            add(dueDateLabel, BorderLayout.EAST);
            
            checkBox = new JCheckBox();
            checkBox.setPreferredSize(new Dimension(32, 28));
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            add(checkBox, BorderLayout.WEST);

            add(new JSeparator(), BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Task> list, Task value, int index, boolean isSelected, boolean cellHasFocus) {
            // Set checkbox based on task completion
            checkBox.setSelected(value.isCompleted());
            checkBox.setEnabled(list.isEnabled());

            label.setText(value.getText());
            dueDateLabel.setText(prettyDateFormat.format(value.getDueDate()));

            // Sed border and background color for selection
            if (isSelected) {
                setBorder(BorderFactory.createLineBorder(new Color(80, 200, 120)));
                setBackground(new Color(210,240,220));
            } else {
                setBorder(null);
                setBackground(new Color(242,242,242));
            }

            return this;
        }
    }
    
    // The content that is displayed with the add task dialog
    private class AddTaskMenu extends JPanel {
        public JTextField descriptionField;
        private JComboBox monthField;
        private JSpinner dayField;
        private JSpinner yearField;
        private JComboBox<String> priorityField;
        
        public AddTaskMenu() {
            initComponents();
        }
        
        // Get date from 3 components
        public Date getDate() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, monthField.getSelectedIndex());
            cal.set(Calendar.YEAR, (Integer) yearField.getValue());
            cal.set(Calendar.DAY_OF_MONTH, (Integer) dayField.getValue());
            
            return cal.getTime();
        }
        
        private void initComponents() {
            Calendar calendar = Calendar.getInstance();
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
            setLayout(new GridLayout(4, 1));
            
            JLabel taskLabel = new JLabel("Task: ");
            taskLabel.setFont(new Font(taskLabel.getFont().getName(), Font.BOLD, 12));
            add(taskLabel);
            
            descriptionField = new JTextField();
            add(descriptionField);

            JLabel priorityLabel = new JLabel("Priority: ");
            priorityLabel.setFont(new Font(priorityLabel.getFont().getName(), Font.BOLD, 12));
            add(priorityLabel);
    
            priorityField = new JComboBox<>(priorityValues);
            add(priorityField);
            
            JLabel dateLabel = new JLabel("Date: ");
            dateLabel.setFont(new Font(dateLabel.getFont().getName(), Font.BOLD, 12));
            add(dateLabel);
            
            monthField = new JComboBox<>(months);
            monthField.setSelectedIndex(currentMonth);
            
            SpinnerNumberModel dayModel = new SpinnerNumberModel(1 , 1, 31, 1);
            dayField = new JSpinner(dayModel);
            dayField.setValue(currentDay); 
            
            SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear , currentYear - 100, currentYear + 100, 1);
            yearField = new JSpinner(yearModel);
            yearField.setEditor(new JSpinner.NumberEditor(yearField, "#"));
            
            JPanel datePicker = new JPanel();
            datePicker.setLayout(new BoxLayout(datePicker, BoxLayout.X_AXIS));
            datePicker.add(monthField);
            datePicker.add(dayField);
            datePicker.add(yearField);
            
            add(datePicker);
        }
    }
    
    // Should be called whenever tasks change
    private void tasksChangedUpdate() {
        saveTasksToFile(); 
        updateCalendar();
    }
    
    // Show a custom dialog for adding a task
    private void showAddTaskMenu() {
        AddTaskMenu dialogPanel = new AddTaskMenu();

        int result = JOptionPane.showConfirmDialog(
            TodoListApp.this,
            dialogPanel,
            "Add a Task",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String taskDescription = dialogPanel.descriptionField.getText();

            if (!taskDescription.isEmpty()) {
                Date dueDate = dialogPanel.getDate();
                String priority = (String) dialogPanel.priorityField.getSelectedItem();
                Task task = new Task(taskDescription, false, dueDate, priority);
                toDoListModel.addElement(task);
                originalTasks.add(task); // Add the task to originalTasks
                sortTasks();
                tasksChangedUpdate();
            }
        }
    }
    
    private void sortTasks() {
        toDoListModel.clear();
        Collections.sort(originalTasks, (Task t1, Task t2) -> {
            if (t1.isCompleted() != t2.isCompleted())
                return t1.isCompleted() ? 1 : -1;
            else
                return (t1.dueDate.getTime() - t2.dueDate.getTime() > 0) ? 1 : -1;
        });

        for (Task task : originalTasks)toDoListModel.addElement(task);
    }

    // Options for filtering tasks
    private class FilterOptions {
        public boolean showComplete = true;
        public boolean showIncomplete = true;
        public boolean clearSelection = true;
        public Date date = null;
        public String priority = null;
        
        public FilterOptions() { }
        public FilterOptions(Date date) {
            this.date = date;
            this.clearSelection = false;
        }
        public FilterOptions(boolean showComplete, boolean showIncomplete) {
            this.showComplete = showComplete;
            this.showIncomplete = showIncomplete;
        }
        public FilterOptions(Date date, String priority){
            this.date = date;
            this.priority = priority;
            this.clearSelection = false;
        }
    }
    
    // Filter tasks based on provided options
    // Filter tasks based on provided options
    private void filterTasks(FilterOptions options) {
        toDoListModel.clear();
        if (options.clearSelection) calendarTable.clearSelection();

        for (Task task : originalTasks) {
            boolean matchesDate = options.date == null || isSameDay(options.date, task.dueDate);
            boolean matchesPriority = options.priority == null || options.priority.equalsIgnoreCase(task.getPriority());

            if (matchesDate && matchesPriority) {
                if ((options.showComplete && task.isCompleted()) || (options.showIncomplete && !task.isCompleted())) {
                    toDoListModel.addElement(task);
                }
            }
        }
    }
    
    // Remove currently selected task
    private void removeSelectedTask() {
        int selectedIndex = toDoList.getSelectedIndex();
        if (selectedIndex != -1) {
            Task removedTask = toDoListModel.getElementAt(selectedIndex); // Get the selected task
            toDoListModel.remove(selectedIndex); // Remove from the list model
            originalTasks.remove(removedTask); // Remove from the originalTasks ArrayList
            tasksChangedUpdate();
        }
    }
    
    // Save tasks to tasks.txt in this directoty
    private void saveTasksToFile() {
        try (PrintWriter writer = new PrintWriter("tasks.txt")) {
            for (int i = 0; i < originalTasks.size(); i++) {
                Task task = originalTasks.get(i);
                String taskText = task.getText();
                boolean completed = task.isCompleted(); // Get the completion status
                Date dueDate = task.getDueDate();
                String dueDateString = dateFormat.format(dueDate);
                String priority = task.getPriority();
                writer.println(taskText + "|" + completed + "|" + dueDateString + "|" + priority); // Include completion status
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    // Load tasks from tasks.txt in this directory
    private void loadTasksFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("tasks.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 4) {  // Change this line
                    String taskText = parts[0];
                    boolean completed = Boolean.parseBoolean(parts[1]);
                    
                    // Parse dueDate
                    Date dueDate = dateFormat.parse(parts[2]);
    
                    // Parse priority
                    String priority = parts[3];
                    
                    Task task = new Task(taskText, completed, dueDate, priority);
                    toDoListModel.addElement(task);
                    originalTasks.add(task);
                }
            }
            sortTasks();
            toDoList.repaint();
        } catch (IOException | ParseException e) {
            e.printStackTrace(System.err);
        }
    }
    
    // Check if two dates are on the same day
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
            && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
            && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
}
