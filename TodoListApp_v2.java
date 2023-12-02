import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class TodoListApp extends JFrame {
    private List<Task> tasks;
    private JPanel taskPanel;
    private JScrollPane scrollPane;

    public TodoListApp() {
        tasks = new ArrayList<>();

        // Set up main frame
        setTitle("To-Do List");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set up buttons
        JButton addTaskButton = new JButton("Add Task");
        JButton removeTaskButton = new JButton("Remove Task");
        JButton showCompletedButton = new JButton("Show Completed");
        JButton showIncompleteButton = new JButton("Show Incomplete");
        JButton showAllButton = new JButton("Show All");

        // Set up task panel
        taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.Y_AXIS));

        // Set up scroll pane
        scrollPane = new JScrollPane(taskPanel);

        // Add buttons to main frame
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 5));
        buttonPanel.add(addTaskButton);
        buttonPanel.add(removeTaskButton);
        buttonPanel.add(showCompletedButton);
        buttonPanel.add(showIncompleteButton);
        buttonPanel.add(showAllButton);

        // Add action listeners to buttons
        addTaskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddTaskDialog();
            }
        });

        // Add components to main frame
        add(buttonPanel, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);

        // Set visible
        setVisible(true);
    }

    private void showAddTaskDialog() {
        JTextField taskDescriptionField = new JTextField();
        Object[] message = {
                "Task Description:", taskDescriptionField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Task", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String taskDescription = taskDescriptionField.getText();
            if (!taskDescription.isEmpty()) {
                Task newTask = new Task(taskDescription);
                tasks.add(newTask);
                updateTaskPanel();
            }
        }
    }

    private void updateTaskPanel() {
        taskPanel.removeAll();

        for (Task task : tasks) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(task.isCompleted());
            checkBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    task.setCompleted(checkBox.isSelected());
                }
            });

            JLabel taskLabel = new JLabel(task.getDescription());

            JPanel taskPane = new JPanel();
            taskPane.setLayout(new BorderLayout());
            taskPane.add(checkBox, BorderLayout.WEST);
            taskPane.add(taskLabel, BorderLayout.CENTER);

            taskPanel.add(taskPane);
        }

        taskPanel.revalidate();
        taskPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TodoListApp();
            }
        });
    }
}

class Task {
    private String description;
    private boolean completed;

    public Task(String description) {
        this.description = description;
        this.completed = false;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}

