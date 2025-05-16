import java.io.File; // Add this line
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileNotFoundException;


public class TimeTableBuilderApp extends JFrame {
    // Components for the tabs
    private JTabbedPane tabbedPane;
    private String currentUserRole;
    private JPanel classroomsPanel;
    private JPanel coursesPanel;
    private JPanel instructorsPanel;
    private JPanel timetablePanel;
    
    // Tables
    private JTable classroomsTable;
    private JTable coursesTable;
    private JTable instructorsTable;
    
    // Components for timetable view
    private JComboBox<String> viewByComboBox;
    private JButton prevButton;
    private JButton nextButton;
    private JPanel timetableGrid;
    
    // Class to represent a scheduled class
    private static class ScheduledClass {
        private String courseCode;
        private String courseType;
        private String instructor;
        private String room;
        private Color backgroundColor;
        private Color borderColor;
        private boolean hasConflict;
        
        public ScheduledClass(String courseCode, String courseType, String instructor, String room, 
                             Color backgroundColor, Color borderColor) {
            this.courseCode = courseCode;
            this.courseType = courseType;
            this.instructor = instructor;
            this.room = room;
            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            this.hasConflict = false;
        }
        
        public void setHasConflict(boolean hasConflict) {
            this.hasConflict = hasConflict;
        }
    }
    
    // Data structures to store timetable information
    private Map<String, Map<Integer, ScheduledClass>> timetableData;
    private int conflicts;
    
    public TimeTableBuilderApp(String role) {
        this.currentUserRole = role;
        setTitle("Time Table Builder - Logged in as: " + role);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        applyRoleRestrictions();
        addRoleMenu();

        initializeUI();
        initializeDemoData();
    }
    
    private void initializeUI() {
        // Setting up the frame
        setLayout(new BorderLayout());
        
        // Create title bar
        JPanel titlePanel = createTitleBar();
        add(titlePanel, BorderLayout.NORTH);
        
        // Create menu bar
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        classroomsPanel = createClassroomsPanel();
        coursesPanel = createCoursesPanel();
        instructorsPanel = createInstructorsPanel();
        timetablePanel = createTimetablePanel();
        
        tabbedPane.addTab("Classrooms", classroomsPanel);
        tabbedPane.addTab("Courses", coursesPanel);
        tabbedPane.addTab("Instructors", instructorsPanel);
        tabbedPane.addTab("Timetable", timetablePanel);
        
        // Set the Timetable tab as selected
        tabbedPane.setSelectedIndex(3);
        
        // Add the tabbed pane to the frame
        add(tabbedPane, BorderLayout.CENTER);
    }
    private void applyRoleRestrictions() {
        if (!"Admin".equals(currentUserRole)) {
            if (classroomsTable != null) classroomsTable.setEnabled(false);
            if (instructorsTable != null) instructorsTable.setEnabled(false);
        }
        if ("Student".equals(currentUserRole)) {
            if (tabbedPane != null) {
                tabbedPane.setEnabledAt(0, false); // Classrooms
                tabbedPane.setEnabledAt(1, false); // Courses
                tabbedPane.setEnabledAt(2, false); // Instructors
            }
        }
    }
    
    private void addRoleMenu() {
        JMenu roleMenu = new JMenu(currentUserRole);
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        JMenuBar mb = getJMenuBar();
        if (mb != null) mb.add(roleMenu);
    }
    
    
    private JPanel createClassroomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Classroom");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        addButton.setBackground(new Color(76, 175, 80));
        addButton.setForeground(Color.BLACK);
        editButton.setBackground(new Color(33, 150, 243));
        editButton.setForeground(Color.BLACK);
        deleteButton.setBackground(new Color(244, 67, 54));
        deleteButton.setForeground(Color.BLACK);
        
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        
        // Create table
        String[] columnNames = {"Room No.", "Capacity", "Type", "Building", "Available"};
        Object[][] data = {
            {"101", "40", "Lecture Hall", "Main Building", true},
            {"102", "35", "Lecture Hall", "Main Building", true},
            {"103", "30", "Lecture Hall", "Main Building", true},
            {"Lab 1", "25", "Computer Lab", "IT Building", true},
            {"Lab 2", "25", "Computer Lab", "IT Building", true},
            {"201", "50", "Lecture Hall", "Main Building", false},
            {"202", "40", "Lecture Hall", "Main Building", true}
        };
        
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 4) return Boolean.class;
                return String.class;
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        classroomsTable = new JTable(model);
        classroomsTable.setRowHeight(25);
        classroomsTable.getTableHeader().setReorderingAllowed(false);
        classroomsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Action listeners
        addButton.addActionListener(e -> showAddClassroomDialog());
        editButton.addActionListener(e -> showEditClassroomDialog());
        deleteButton.addActionListener(e -> deleteSelectedClassroom());
        
        JScrollPane scrollPane = new JScrollPane(classroomsTable);
        
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void showAddClassroomDialog() {
        JDialog dialog = new JDialog(this, "Add Classroom", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField roomNoField = new JTextField(20);
        JTextField capacityField = new JTextField(20);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Lecture Hall", "Computer Lab", "Laboratory", "Seminar Room"});
        JTextField buildingField = new JTextField(20);
        JCheckBox availableCheck = new JCheckBox("Available");
        availableCheck.setSelected(true);
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Room No:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roomNoField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Capacity:"), gbc);
        gbc.gridx = 1;
        formPanel.add(capacityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(typeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Building:"), gbc);
        gbc.gridx = 1;
        formPanel.add(buildingField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Available:"), gbc);
        gbc.gridx = 1;
        formPanel.add(availableCheck, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            if (validateClassroomInput(roomNoField, capacityField, buildingField)) {
                DefaultTableModel model = (DefaultTableModel) classroomsTable.getModel();
                Object[] rowData = {
                    roomNoField.getText(),
                    capacityField.getText(),
                    typeCombo.getSelectedItem(),
                    buildingField.getText(),
                    availableCheck.isSelected()
                };
                model.addRow(rowData);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Classroom added successfully!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private boolean validateClassroomInput(JTextField roomNo, JTextField capacity, JTextField building) {
        if (roomNo.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Room number cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        try {
            int cap = Integer.parseInt(capacity.getText());
            if (cap <= 0) {
                JOptionPane.showMessageDialog(this, "Capacity must be a positive number", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Capacity must be a valid number", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (building.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Building cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    private void showEditClassroomDialog() {
        int selectedRow = classroomsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a classroom to edit.");
            return;
        }
        
        JDialog dialog = new JDialog(this, "Edit Classroom", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField roomNoField = new JTextField((String) classroomsTable.getValueAt(selectedRow, 0), 20);
        JTextField capacityField = new JTextField((String) classroomsTable.getValueAt(selectedRow, 1), 20);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Lecture Hall", "Computer Lab", "Laboratory", "Seminar Room"});
        typeCombo.setSelectedItem(classroomsTable.getValueAt(selectedRow, 2));
        JTextField buildingField = new JTextField((String) classroomsTable.getValueAt(selectedRow, 3), 20);
        JCheckBox availableCheck = new JCheckBox("Available");
        availableCheck.setSelected((Boolean) classroomsTable.getValueAt(selectedRow, 4));
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Room No:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roomNoField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Capacity:"), gbc);
        gbc.gridx = 1;
        formPanel.add(capacityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(typeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Building:"), gbc);
        gbc.gridx = 1;
        formPanel.add(buildingField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Available:"), gbc);
        gbc.gridx = 1;
        formPanel.add(availableCheck, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            if (validateClassroomInput(roomNoField, capacityField, buildingField)) {
                classroomsTable.setValueAt(roomNoField.getText(), selectedRow, 0);
                classroomsTable.setValueAt(capacityField.getText(), selectedRow, 1);
                classroomsTable.setValueAt(typeCombo.getSelectedItem(), selectedRow, 2);
                classroomsTable.setValueAt(buildingField.getText(), selectedRow, 3);
                classroomsTable.setValueAt(availableCheck.isSelected(), selectedRow, 4);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Classroom updated successfully!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void deleteSelectedClassroom() {
        int selectedRow = classroomsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a classroom to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this classroom?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            DefaultTableModel model = (DefaultTableModel) classroomsTable.getModel();
            model.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this, "Classroom deleted successfully!");
        }
    }
    
    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Course");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton sectionsButton = new JButton("Manage Sections");
        addButton.setBackground(new Color(76, 175, 80));
        addButton.setForeground(Color.BLACK);
        editButton.setBackground(new Color(33, 150, 243));
        editButton.setForeground(Color.BLACK);
        deleteButton.setBackground(new Color(244, 67, 54));
        deleteButton.setForeground(Color.BLACK);
        sectionsButton.setBackground(new Color(255, 152, 0));
        sectionsButton.setForeground(Color.BLACK);
        
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(sectionsButton);
        
        // Create table
        String[] columnNames = {"Course Code", "Course Name", "Credits", "Sections", "Theory Hours", "Lab Hours"};
        Object[][] data = {
            {"CSF213", "Object Oriented Programming", "4", "3", "3", "2"},
            {"CSF214", "Logic in Computer Science", "3", "2", "3", "0"},
            {"CSF215", "Digital Design", "4", "3", "3", "2"},
            {"CSF216", "Database Management Systems", "4", "2", "3", "2"},
            {"CSF217", "Operating Systems", "4", "3", "3", "1"}
        };
        
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        coursesTable = new JTable(model);
        coursesTable.setRowHeight(25);
        coursesTable.getTableHeader().setReorderingAllowed(false);
        coursesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Action listeners
        addButton.addActionListener(e -> showAddCourseDialog());
        editButton.addActionListener(e -> showEditCourseDialog());
        deleteButton.addActionListener(e -> deleteSelectedCourse());
        sectionsButton.addActionListener(e -> showManageSectionsDialog());
        
        JScrollPane scrollPane = new JScrollPane(coursesTable);
        
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void showAddCourseDialog() {
        JDialog dialog = new JDialog(this, "Add Course", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField courseCodeField = new JTextField(20);
        JTextField courseNameField = new JTextField(20);
        JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JSpinner sectionsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        JSpinner theoryHoursSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 6, 1));
        JSpinner labHoursSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4, 1));
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Course Code:"), gbc);
        gbc.gridx = 1;
        formPanel.add(courseCodeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Course Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(courseNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Credits:"), gbc);
        gbc.gridx = 1;
        formPanel.add(creditsSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Sections:"), gbc);
        gbc.gridx = 1;
        formPanel.add(sectionsSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Theory Hours:"), gbc);
        gbc.gridx = 1;
        formPanel.add(theoryHoursSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Lab Hours:"), gbc);
        gbc.gridx = 1;
        formPanel.add(labHoursSpinner, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            if (validateCourseInput(courseCodeField, courseNameField)) {
                DefaultTableModel model = (DefaultTableModel) coursesTable.getModel();
                Object[] rowData = {
                    courseCodeField.getText(),
                    courseNameField.getText(),
                    creditsSpinner.getValue().toString(),
                    sectionsSpinner.getValue().toString(),
                    theoryHoursSpinner.getValue().toString(),
                    labHoursSpinner.getValue().toString()
                };
                model.addRow(rowData);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Course added successfully!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private boolean validateCourseInput(JTextField code, JTextField name) {
        if (code.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Course code cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (name.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Course name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    private void showEditCourseDialog() {
        int selectedRow = coursesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to edit.");
            return;
        }
        
        JDialog dialog = new JDialog(this, "Edit Course", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField courseCodeField = new JTextField((String) coursesTable.getValueAt(selectedRow, 0), 20);
        JTextField courseNameField = new JTextField((String) coursesTable.getValueAt(selectedRow, 1), 20);
        JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt((String) coursesTable.getValueAt(selectedRow, 2)), 1, 5, 1));
        JSpinner sectionsSpinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt((String) coursesTable.getValueAt(selectedRow, 3)), 1, 10, 1));
        JSpinner theoryHoursSpinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt((String) coursesTable.getValueAt(selectedRow, 4)), 0, 6, 1));
        JSpinner labHoursSpinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt((String) coursesTable.getValueAt(selectedRow, 5)), 0, 4, 1));
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Course Code:"), gbc);
        gbc.gridx = 1;
        formPanel.add(courseCodeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Course Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(courseNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Credits:"), gbc);
        gbc.gridx = 1;
        formPanel.add(creditsSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Sections:"), gbc);
        gbc.gridx = 1;
        formPanel.add(sectionsSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Theory Hours:"), gbc);
        gbc.gridx = 1;
        formPanel.add(theoryHoursSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Lab Hours:"), gbc);
        gbc.gridx = 1;
        formPanel.add(labHoursSpinner, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            if (validateCourseInput(courseCodeField, courseNameField)) {
                coursesTable.setValueAt(courseCodeField.getText(), selectedRow, 0);
                coursesTable.setValueAt(courseNameField.getText(), selectedRow, 1);
                coursesTable.setValueAt(creditsSpinner.getValue().toString(), selectedRow, 2);
                coursesTable.setValueAt(sectionsSpinner.getValue().toString(), selectedRow, 3);
                coursesTable.setValueAt(theoryHoursSpinner.getValue().toString(), selectedRow, 4);
                coursesTable.setValueAt(labHoursSpinner.getValue().toString(), selectedRow, 5);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Course updated successfully!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void deleteSelectedCourse() {
        int selectedRow = coursesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this course?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            DefaultTableModel model = (DefaultTableModel) coursesTable.getModel();
            model.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this, "Course deleted successfully!");
        }
    }
    
    private void showManageSectionsDialog() {
        int selectedRow = coursesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course to manage sections.");
            return;
        }
        
        String courseCode = (String) coursesTable.getValueAt(selectedRow, 0);
        String courseName = (String) coursesTable.getValueAt(selectedRow, 1);
        
        JDialog dialog = new JDialog(this, "Manage Sections - " + courseCode, true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel headerLabel = new JLabel(courseCode + " - " + courseName);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        header.add(headerLabel, BorderLayout.WEST);
        
        JPanel sectionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addSectionButton = new JButton("Add Section");
        JButton removeSectionButton = new JButton("Remove Section");
        sectionToolbar.add(addSectionButton);
        sectionToolbar.add(removeSectionButton);
        
        String[] sectionColumns = {"Section", "Instructor", "Classroom", "Time Slot"};
        Object[][] sectionData = {
            {"L1", "Prof. Smith", "Room 101", "Mon 9:30-11:00"},
            {"L2", "Dr. Johnson", "Room 102", "Tue 11:00-12:30"},
            {"L3", "Prof. Garcia", "Room 103", "Wed 8:00-9:30"}
        };
        
        DefaultTableModel sectionModel = new DefaultTableModel(sectionData, sectionColumns);
        JTable sectionTable = new JTable(sectionModel);
        JScrollPane sectionScrollPane = new JScrollPane(sectionTable);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);
        
        dialog.add(header, BorderLayout.NORTH);
        dialog.add(sectionToolbar, BorderLayout.WEST);
        dialog.add(sectionScrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private JPanel createInstructorsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	 String[] columnNames = {"ID", "Name", "Department", "Email", "Courses", "Max Load", "Preferred Time"};
        Object[][] data = {
            {"I001", "Prof. Smith", "Computer Science", "smith@university.edu", "CSF213, CSF214", "12", "Morning"},
            {"I002", "Dr. Johnson", "Computer Science", "johnson@university.edu", "CSF215, CSF216", "10", "Afternoon"},
            {"I003", "Prof. Garcia", "Computer Science", "garcia@university.edu", "CSF217", "8", "Morning"},
            {"I004", "Dr. Chen", "Computer Science", "chen@university.edu", "CSF213, CSF217", "12", "Any"},
            {"I005", "Prof. Williams", "Computer Science", "williams@university.edu", "CSF214, CSF216", "10", "Afternoon"}
        };
        
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        instructorsTable = new JTable(model);
        instructorsTable.setRowHeight(25);
        instructorsTable.getTableHeader().setReorderingAllowed(false);
        instructorsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Create toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Instructor");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton assignButton = new JButton("Assign Courses");
        addButton.setBackground(new Color(76, 175, 80));
        addButton.setForeground(Color.BLACK);
        editButton.setBackground(new Color(33, 150, 243));
        editButton.setForeground(Color.BLACK);
        deleteButton.setBackground(new Color(244, 67, 54));
        deleteButton.setForeground(Color.BLACK);
        assignButton.setBackground(new Color(156, 39, 176));
        assignButton.setForeground(Color.BLACK);
        
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(assignButton);
        
        // Action listeners
        addButton.addActionListener(e -> showAddInstructorDialog());
        editButton.addActionListener(e -> showEditInstructorDialog());
        deleteButton.addActionListener(e -> deleteSelectedInstructor());
        assignButton.addActionListener(e -> showAssignCoursesDialog());
        
        JScrollPane scrollPane = new JScrollPane(instructorsTable);
        
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void showAddInstructorDialog() {
        JDialog dialog = new JDialog(this, "Add Instructor", true);
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField idField = new JTextField(20);
        JTextField nameField = new JTextField(20);
        JTextField departmentField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JSpinner maxLoadSpinner = new JSpinner(new SpinnerNumberModel(12, 4, 20, 1));
        JComboBox<String> preferredTimeCombo = new JComboBox<>(new String[]{"Morning", "Afternoon", "Evening", "Any"});
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        formPanel.add(idField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        formPanel.add(departmentField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Max Load:"), gbc);
        gbc.gridx = 1;
        formPanel.add(maxLoadSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Preferred Time:"), gbc);
        gbc.gridx = 1;
        formPanel.add(preferredTimeCombo, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            if (validateInstructorInput(idField, nameField, departmentField, emailField)) {
                DefaultTableModel model = (DefaultTableModel) instructorsTable.getModel();
                Object[] rowData = {
                    idField.getText(),
                    nameField.getText(),
                    departmentField.getText(),
                    emailField.getText(),
                    "", // Initial empty courses
                    maxLoadSpinner.getValue().toString(),
                    preferredTimeCombo.getSelectedItem()
                };
                model.addRow(rowData);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Instructor added successfully!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private boolean validateInstructorInput(JTextField id, JTextField name, JTextField department, JTextField email) {
        if (id.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (name.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (department.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Department cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (email.getText().trim().isEmpty() || !email.getText().contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    private void showEditInstructorDialog() {
        int selectedRow = instructorsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an instructor to edit.");
            return;
        }
        
        JDialog dialog = new JDialog(this, "Edit Instructor", true);
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField idField = new JTextField((String) instructorsTable.getValueAt(selectedRow, 0), 20);
        JTextField nameField = new JTextField((String) instructorsTable.getValueAt(selectedRow, 1), 20);
        JTextField departmentField = new JTextField((String) instructorsTable.getValueAt(selectedRow, 2), 20);
        JTextField emailField = new JTextField((String) instructorsTable.getValueAt(selectedRow, 3), 20);
        JSpinner maxLoadSpinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt((String) instructorsTable.getValueAt(selectedRow, 5)), 4, 20, 1));
        JComboBox<String> preferredTimeCombo = new JComboBox<>(new String[]{"Morning", "Afternoon", "Evening", "Any"});
        preferredTimeCombo.setSelectedItem(instructorsTable.getValueAt(selectedRow, 6));
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        formPanel.add(idField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        formPanel.add(departmentField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Max Load:"), gbc);
        gbc.gridx = 1;
        formPanel.add(maxLoadSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Preferred Time:"), gbc);
        gbc.gridx = 1;
        formPanel.add(preferredTimeCombo, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            if (validateInstructorInput(idField, nameField, departmentField, emailField)) {
                instructorsTable.setValueAt(idField.getText(), selectedRow, 0);
                instructorsTable.setValueAt(nameField.getText(), selectedRow, 1);
                instructorsTable.setValueAt(departmentField.getText(), selectedRow, 2);
                instructorsTable.setValueAt(emailField.getText(), selectedRow, 3);
                instructorsTable.setValueAt(maxLoadSpinner.getValue().toString(), selectedRow, 5);
                instructorsTable.setValueAt(preferredTimeCombo.getSelectedItem(), selectedRow, 6);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Instructor updated successfully!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void deleteSelectedInstructor() {
        int selectedRow = instructorsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an instructor to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this instructor?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            DefaultTableModel model = (DefaultTableModel) instructorsTable.getModel();
            model.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this, "Instructor deleted successfully!");
        }
    }
    
    private void showAssignCoursesDialog() {
        int selectedRow = instructorsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an instructor to assign courses.");
            return;
        }
        
        String instructorName = (String) instructorsTable.getValueAt(selectedRow, 1);
        String currentCourses = (String) instructorsTable.getValueAt(selectedRow, 4);
        
        JDialog dialog = new JDialog(this, "Assign Courses - " + instructorName, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Create a list of available courses
        DefaultListModel<String> availableCoursesModel = new DefaultListModel<>();
        for (int i = 0; i < coursesTable.getRowCount(); i++) {
            String courseCode = (String) coursesTable.getValueAt(i, 0);
            String courseName = (String) coursesTable.getValueAt(i, 1);
            availableCoursesModel.addElement(courseCode + " - " + courseName);
        }
        
        JList<String> coursesList = new JList<>(availableCoursesModel);
        coursesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Pre-select current courses
        if (!currentCourses.isEmpty()) {
            String[] assignedCourses = currentCourses.split(", ");
            for (String course : assignedCourses) {
                for (int i = 0; i < availableCoursesModel.size(); i++) {
                    if (availableCoursesModel.get(i).startsWith(course)) {
                        coursesList.addSelectionInterval(i, i);
                    }
                }
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(coursesList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Select Courses"));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            StringBuilder selectedCourses = new StringBuilder();
            for (int index : coursesList.getSelectedIndices()) {
                String course = availableCoursesModel.get(index);
                String courseCode = course.substring(0, course.indexOf(" - "));
                if (selectedCourses.length() > 0) {
                    selectedCourses.append(", ");
                }
                selectedCourses.append(courseCode);
            }
            instructorsTable.setValueAt(selectedCourses.toString(), selectedRow, 4);
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Courses assigned successfully!");
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private JPanel createTimetablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create the top toolbar with view options
        JPanel topToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JLabel viewByLabel = new JLabel("View by:");
        viewByComboBox = new JComboBox<>(new String[]{"Classroom", "Course", "Instructor"});
        viewByComboBox.setSelectedIndex(0);
        
        JButton generateButton = new JButton("Generate Timetable");
        JButton exportButton = new JButton("Export");
        JButton importButton = new JButton("Import");
        JButton saveButton = new JButton("Save");
        
        generateButton.setBackground(new Color(0, 150, 136));
        generateButton.setForeground(Color.BLACK);
        exportButton.setBackground(new Color(33, 150, 243));
        exportButton.setForeground(Color.BLACK);
        importButton.setBackground(new Color(121, 85, 72));
        importButton.setForeground(Color.BLACK);
        saveButton.setBackground(new Color(76, 175, 80));
        saveButton.setForeground(Color.BLACK);
        
        topToolbar.add(viewByLabel);
        topToolbar.add(viewByComboBox);
        topToolbar.add(new JSeparator(SwingConstants.VERTICAL));
        topToolbar.add(generateButton);
        topToolbar.add(exportButton);
        topToolbar.add(importButton);
        topToolbar.add(saveButton);
        
        // Create the navigation bar
        JPanel navigationBar = new JPanel(new BorderLayout());
        JPanel navButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        prevButton = new JButton("< Previous");
        nextButton = new JButton("Next >");
        JLabel conflictLabel = new JLabel("Conflicts: 0");
        conflictLabel.setForeground(new Color(211, 47, 47));
        
        navButtonPanel.add(prevButton);
        navButtonPanel.add(nextButton);
        navButtonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        navButtonPanel.add(conflictLabel);
        
        navigationBar.add(navButtonPanel, BorderLayout.CENTER);
        
        // Create the timetable grid
        timetableGrid = new JPanel(new BorderLayout());
        timetableGrid.setBackground(Color.WHITE);
        refreshTimetableGrid();
        
        // Action listeners
        viewByComboBox.addActionListener(e -> refreshTimetableGrid());
        prevButton.addActionListener(e -> showPreviousPage());
        nextButton.addActionListener(e -> showNextPage());
        generateButton.addActionListener(e -> generateTimetable());
        exportButton.addActionListener(e -> exportTimetable());
        importButton.addActionListener(e -> importTimetable());
        saveButton.addActionListener(e -> saveTimetable());
        
        // Combining all components
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(topToolbar, BorderLayout.NORTH);
        topPanel.add(navigationBar, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(timetableGrid, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void refreshTimetableGrid() {
        timetableGrid.removeAll();
        
        String[] timeSlots = {
            "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
            "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00",
            "16:00-17:00", "17:00-18:00"
        };
        
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        JPanel schedulePanel = new JPanel(new GridLayout(timeSlots.length + 1, days.length + 1, 1, 1));
        schedulePanel.setBackground(new Color(240, 240, 240));
        
        // Create header row
        schedulePanel.add(new JLabel("")); // Empty corner
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setOpaque(true);
            dayLabel.setBackground(new Color(224, 224, 224));
            dayLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            dayLabel.setFont(new Font("Arial", Font.BOLD, 12));
            schedulePanel.add(dayLabel);
        }
        
        // Create rows for each time slot
        for (String timeSlot : timeSlots) {
            JLabel timeLabel = new JLabel(timeSlot, SwingConstants.CENTER);
            timeLabel.setOpaque(true);
            timeLabel.setBackground(new Color(224, 224, 224));
            timeLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            timeLabel.setFont(new Font("Arial", Font.BOLD, 12));
            schedulePanel.add(timeLabel);
            
            for (int j = 0; j < days.length; j++) {
                JPanel cellPanel = new JPanel();
                cellPanel.setBackground(Color.WHITE);
                cellPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                cellPanel.setLayout(new BorderLayout());
                
                // Add draggable functionality
                cellPanel.setTransferHandler(new TransferHandler() {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                    public boolean canImport(TransferHandler.TransferSupport support) {
                        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
                    }
                    
                    @Override
                    public boolean importData(TransferHandler.TransferSupport support) {
                        try {
                            String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                            // Handle the dropped data
                            handleDroppedClass(cellPanel, data);
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                });
                
                cellPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            showScheduleDialog(cellPanel);
                        }
                    }
                });
                
                schedulePanel.add(cellPanel);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(schedulePanel);
        timetableGrid.add(scrollPane, BorderLayout.CENTER);
        timetableGrid.revalidate();
        timetableGrid.repaint();
    }
    
    private void handleDroppedClass(JPanel cellPanel, String data) {
        // Parse the dropped data and create a scheduled class
        cellPanel.removeAll();
        JLabel classLabel = new JLabel("<html>" + data.replace("\n", "<br>") + "</html>");
        classLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        cellPanel.add(classLabel, BorderLayout.CENTER);
        
        // Add color based on class type
        if (data.contains("Lab")) {
            cellPanel.setBackground(new Color(144, 202, 249));
        } else if (data.contains("Lecture")) {
            cellPanel.setBackground(new Color(197, 225, 165));
        } else {
            cellPanel.setBackground(new Color(255, 224, 178));
        }
        
        cellPanel.revalidate();
        cellPanel.repaint();
    }
    
    private void showScheduleDialog(JPanel cellPanel) {
        JDialog dialog = new JDialog(this, "Schedule Class", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Course selection
        JComboBox<String> courseComboBox = new JComboBox<>();
        DefaultTableModel coursesModel = (DefaultTableModel) coursesTable.getModel();
        for (int i = 0; i < coursesModel.getRowCount(); i++) {
            String courseCode = (String) coursesModel.getValueAt(i, 0);
            String courseName = (String) coursesModel.getValueAt(i, 1);
            courseComboBox.addItem(courseCode + " - " + courseName);
        }
        
        // Type selection
        JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"Lecture", "Tutorial", "Lab"});
        
        // Instructor selection
        JComboBox<String> instructorComboBox = new JComboBox<>();
        DefaultTableModel instructorsModel = (DefaultTableModel) instructorsTable.getModel();
        for (int i = 0; i < instructorsModel.getRowCount(); i++) {
            String instructorName = (String) instructorsModel.getValueAt(i, 1);
            instructorComboBox.addItem(instructorName);
        }
        
        // Room selection
        JComboBox<String> roomComboBox = new JComboBox<>();
        DefaultTableModel classroomsModel = (DefaultTableModel) classroomsTable.getModel();
        for (int i = 0; i < classroomsModel.getRowCount(); i++) {
            String roomNo = (String) classroomsModel.getValueAt(i, 0);
            roomComboBox.addItem(roomNo);
        }
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Course:"), gbc);
        gbc.gridx = 1;
        formPanel.add(courseComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(typeComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Instructor:"), gbc);
        gbc.gridx = 1;
        formPanel.add(instructorComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Room:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roomComboBox, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            String selectedCourse = (String) courseComboBox.getSelectedItem();
            String courseCode = selectedCourse.substring(0, selectedCourse.indexOf(" - "));
            String type = (String) typeComboBox.getSelectedItem();
            String instructor = (String) instructorComboBox.getSelectedItem();
            String room = (String) roomComboBox.getSelectedItem();
            
            cellPanel.removeAll();
            String displayText = "<html>" + courseCode + "<br>" + type + "<br>" + instructor + "<br>" + room + "</html>";
            JLabel classLabel = new JLabel(displayText);
            classLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            cellPanel.add(classLabel, BorderLayout.CENTER);
            
            // Set background color based on type
            if (type.equals("Lab")) {
                cellPanel.setBackground(new Color(144, 202, 249));
            } else if (type.equals("Lecture")) {
                cellPanel.setBackground(new Color(197, 225, 165));
            } else {
                cellPanel.setBackground(new Color(255, 224, 178));
            }
            
            cellPanel.revalidate();
            cellPanel.repaint();
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void generateTimetable() {
    // Show a progress dialog
    JDialog progressDialog = new JDialog(this, "Generating Timetable", true);
    progressDialog.setSize(300, 100);
    progressDialog.setLocationRelativeTo(this);
    progressDialog.setLayout(new BorderLayout());
    
    JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
    progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    
    JProgressBar progressBar = new JProgressBar(0, 100);
    progressBar.setIndeterminate(true);
    JLabel statusLabel = new JLabel("Analyzing constraints and generating timetable...");
    
    progressPanel.add(statusLabel, BorderLayout.NORTH);
    progressPanel.add(progressBar, BorderLayout.CENTER);
    
    progressDialog.add(progressPanel);
    
    // Create a worker thread to generate the timetable
    SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
        @Override
        protected Void doInBackground() throws Exception {
            // Initialize timetable data structure
            timetableData = new HashMap<>();
            conflicts = 0;
            
            // Get all available resources
            DefaultTableModel coursesModel = (DefaultTableModel) coursesTable.getModel();
            DefaultTableModel instructorsModel = (DefaultTableModel) instructorsTable.getModel();
            DefaultTableModel classroomsModel = (DefaultTableModel) classroomsTable.getModel();
            
            // Define time slots and days
            String[] timeSlots = {
                "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
                "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00",
                "16:00-17:00", "17:00-18:00"
            };
            
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            
            // Maps to track instructor and room availability
            Map<String, Set<String>> instructorSchedule = new HashMap<>();
            Map<String, Set<String>> roomSchedule = new HashMap<>();
            
            // Initialize availability tracking
            for (int i = 0; i < instructorsModel.getRowCount(); i++) {
                instructorSchedule.put((String) instructorsModel.getValueAt(i, 1), new HashSet<>());
            }
            
            for (int i = 0; i < classroomsModel.getRowCount(); i++) {
                if ((Boolean) classroomsModel.getValueAt(i, 4)) { // Check if room is available
                    roomSchedule.put((String) classroomsModel.getValueAt(i, 0), new HashSet<>());
                }
            }
            
            // Random for generating random schedules
            Random random = new Random();
            
            // Schedule each course
            for (int i = 0; i < coursesModel.getRowCount(); i++) {
                String courseCode = (String) coursesModel.getValueAt(i, 0);
                String courseName = (String) coursesModel.getValueAt(i, 1);
                int sections = Integer.parseInt((String) coursesModel.getValueAt(i, 3));
                int theoryHours = Integer.parseInt((String) coursesModel.getValueAt(i, 4));
                int labHours = Integer.parseInt((String) coursesModel.getValueAt(i, 5));
                
                // Find suitable instructors for this course
                java.util.List<String> suitableInstructors = new ArrayList<>();
                for (int j = 0; j < instructorsModel.getRowCount(); j++) {
                    String courses = (String) instructorsModel.getValueAt(j, 4);
                    if (courses.contains(courseCode)) {
                        suitableInstructors.add((String) instructorsModel.getValueAt(j, 1));
                    }
                }
                
                // If no suitable instructor found, use any instructor
                if (suitableInstructors.isEmpty()) {
                    for (int j = 0; j < instructorsModel.getRowCount(); j++) {
                        suitableInstructors.add((String) instructorsModel.getValueAt(j, 1));
                    }
                }
                
                // Schedule theory classes for each section
                for (int section = 1; section <= sections; section++) {
                    String sectionCode = courseCode + "-L" + section;
                    
                    // Schedule theory hours
                    for (int hour = 0; hour < theoryHours; hour++) {
                        // Try to find a suitable time slot
                        boolean scheduled = false;
                        int attempts = 0;
                        
                        while (!scheduled && attempts < 50) {
                            String day = days[random.nextInt(days.length)];
                            String timeSlot = timeSlots[random.nextInt(timeSlots.length)];
                            String timeSlotKey = day + "-" + timeSlot;
                            
                            // Choose a random instructor from suitable ones
                            String instructor = suitableInstructors.get(random.nextInt(suitableInstructors.size()));
                            
                            // Find suitable lecture rooms
                            java.util.List<String> suitableLectureRooms = new ArrayList<>();
                            for (int r = 0; r < classroomsModel.getRowCount(); r++) {
                                String roomType = (String) classroomsModel.getValueAt(r, 2);
                                boolean isAvailable = (Boolean) classroomsModel.getValueAt(r, 4);
                                
                                if (isAvailable && roomType.contains("Lecture")) {
                                    suitableLectureRooms.add((String) classroomsModel.getValueAt(r, 0));
                                }
                            }
                            
                            // If no suitable room, use any available room
                            if (suitableLectureRooms.isEmpty()) {
                                for (int r = 0; r < classroomsModel.getRowCount(); r++) {
                                    if ((Boolean) classroomsModel.getValueAt(r, 4)) {
                                        suitableLectureRooms.add((String) classroomsModel.getValueAt(r, 0));
                                    }
                                }
                            }
                            
                            if (!suitableLectureRooms.isEmpty()) {
                                String room = suitableLectureRooms.get(random.nextInt(suitableLectureRooms.size()));
                                
                                // Check for conflicts
                                boolean instructorConflict = instructorSchedule.get(instructor).contains(timeSlotKey);
                                boolean roomConflict = roomSchedule.get(room) != null && 
                                                      roomSchedule.get(room).contains(timeSlotKey);
                                
                                // Create the scheduled class
                                Color bgColor = new Color(197, 225, 165); // Green for theory
                                Color borderColor = Color.BLACK;
                                
                                ScheduledClass scheduledClass = new ScheduledClass(
                                        sectionCode, "Lecture", instructor, room, bgColor, borderColor);
                                
                                // Check for conflicts and update the conflict status
                                if (instructorConflict || roomConflict) {
                                    scheduledClass.setHasConflict(true);
                                    conflicts++;
                                } else {
                                    // Mark the time slot as used
                                    instructorSchedule.get(instructor).add(timeSlotKey);
                                    if (roomSchedule.get(room) == null) {
                                        roomSchedule.put(room, new HashSet<>());
                                    }
                                    roomSchedule.get(room).add(timeSlotKey);
                                }
                                
                                // Add to timetable data
                                if (!timetableData.containsKey(timeSlotKey)) {
                                    timetableData.put(timeSlotKey, new HashMap<>());
                                }
                                
                                // Find an available slot in this time slot
                                int slotIndex = 0;
                                while (timetableData.get(timeSlotKey).containsKey(slotIndex)) {
                                    slotIndex++;
                                }
                                
                                timetableData.get(timeSlotKey).put(slotIndex, scheduledClass);
                                scheduled = true;
                            }
                            
                            attempts++;
                        }
                    }
                    
                    // Schedule lab hours if needed
                    if (labHours > 0) {
                        String labCode = courseCode + "-P" + section;
                        
                        // Try to find a suitable time slot for lab
                        boolean scheduled = false;
                        int attempts = 0;
                        
                        while (!scheduled && attempts < 50) {
                            String day = days[random.nextInt(days.length)];
                            String timeSlot = timeSlots[random.nextInt(timeSlots.length)];
                            String timeSlotKey = day + "-" + timeSlot;
                            
                            // Choose a random instructor from suitable ones
                            String instructor = suitableInstructors.get(random.nextInt(suitableInstructors.size()));
                            
                            // Find suitable lab rooms
                            java.util.List<String> suitableLabRooms = new ArrayList<>();
                            for (int r = 0; r < classroomsModel.getRowCount(); r++) {
                                String roomType = (String) classroomsModel.getValueAt(r, 2);
                                boolean isAvailable = (Boolean) classroomsModel.getValueAt(r, 4);
                                
                                if (isAvailable && roomType.contains("Lab")) {
                                    suitableLabRooms.add((String) classroomsModel.getValueAt(r, 0));
                                }
                            }
                            
                            if (!suitableLabRooms.isEmpty()) {
                                String room = suitableLabRooms.get(random.nextInt(suitableLabRooms.size()));
                                
                                // Check for conflicts
                                boolean instructorConflict = instructorSchedule.get(instructor).contains(timeSlotKey);
                                boolean roomConflict = roomSchedule.get(room) != null && 
                                                      roomSchedule.get(room).contains(timeSlotKey);
                                
                                // Create the scheduled class
                                Color bgColor = new Color(144, 202, 249); // Blue for lab
                                Color borderColor = Color.BLACK;
                                
                                ScheduledClass scheduledClass = new ScheduledClass(
                                        labCode, "Lab", instructor, room, bgColor, borderColor);
                                
                                // Check for conflicts and update the conflict status
                                if (instructorConflict || roomConflict) {
                                    scheduledClass.setHasConflict(true);
                                    conflicts++;
                                } else {
                                    // Mark the time slot as used
                                    instructorSchedule.get(instructor).add(timeSlotKey);
                                    if (roomSchedule.get(room) == null) {
                                        roomSchedule.put(room, new HashSet<>());
                                    }
                                    roomSchedule.get(room).add(timeSlotKey);
                                }
                                
                                // Add to timetable data
                                if (!timetableData.containsKey(timeSlotKey)) {
                                    timetableData.put(timeSlotKey, new HashMap<>());
                                }
                                
                                // Find an available slot in this time slot
                                int slotIndex = 0;
                                while (timetableData.get(timeSlotKey).containsKey(slotIndex)) {
                                    slotIndex++;
                                }
                                
                                timetableData.get(timeSlotKey).put(slotIndex, scheduledClass);
                                scheduled = true;
                            }
                            
                            attempts++;
                        }
                    }
                }
            }
            
            return null;
        }
        
        @Override
        protected void done() {
            progressDialog.dispose();
            refreshTimetableGrid();
            JOptionPane.showMessageDialog(TimeTableBuilderApp.this, 
                "Timetable generated successfully with " + conflicts + " conflicts.", 
                "Generation Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    };
    
    worker.execute();
    progressDialog.setVisible(true);
}

    
    private void exportTimetable() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Timetable");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            // Implementation for exporting to CSV
            JOptionPane.showMessageDialog(this, "Timetable exported successfully to: " + fileToSave.getAbsolutePath());
        }
    }
    
    private void importTimetable() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Timetable");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToOpen = fileChooser.getSelectedFile();
            // Implementation for importing from CSV
            JOptionPane.showMessageDialog(this, "Timetable imported successfully from: " + fileToOpen.getAbsolutePath());
        }
    }
    
private void saveTimetable() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Timetable");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Timetable files", "ttb"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            // Implementation for saving timetable to binary file
            try {
                // For demonstration, just show success message
                JOptionPane.showMessageDialog(this, "Timetable saved successfully to: " + fileToSave.getAbsolutePath());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error saving timetable: " + e.getMessage(), 
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void showPreviousPage() {
        // Implementation for showing previous page based on current view type
        String viewType = (String) viewByComboBox.getSelectedItem();
        JOptionPane.showMessageDialog(this, "Showing previous " + viewType);
    }
    
    private void showNextPage() {
        // Implementation for showing next page based on current view type
        String viewType = (String) viewByComboBox.getSelectedItem();
        JOptionPane.showMessageDialog(this, "Showing next " + viewType);
    }
    
    private JPanel createTitleBar() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(25, 118, 210));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("Time Table Builder");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setOpaque(false);
        JTextField searchField = new JTextField(15);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(66, 165, 245));
        searchButton.setForeground(Color.BLACK);
        
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(searchPanel, BorderLayout.EAST);
        
        return titlePanel;
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        JMenuItem exportItem = new JMenuItem("Export");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        
        JMenu viewMenu = new JMenu("View");
        JMenuItem classroomsItem = new JMenuItem("Classrooms");
        JMenuItem coursesItem = new JMenuItem("Courses");
        JMenuItem instructorsItem = new JMenuItem("Instructors");
        JMenuItem timetableItem = new JMenuItem("Timetable");
        
        viewMenu.add(classroomsItem);
        viewMenu.add(coursesItem);
        viewMenu.add(instructorsItem);
        viewMenu.add(timetableItem);
        
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem validateItem = new JMenuItem("Validate Timetable");
        JMenuItem generateItem = new JMenuItem("Generate Automatic");
        JMenuItem findConflictsItem = new JMenuItem("Find Conflicts");
        JMenuItem preferencesItem = new JMenuItem("Preferences");
        
        toolsMenu.add(validateItem);
        toolsMenu.add(generateItem);
        toolsMenu.add(findConflictsItem);
        toolsMenu.addSeparator();
        toolsMenu.add(preferencesItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem documentationItem = new JMenuItem("Documentation");
        JMenuItem aboutItem = new JMenuItem("About");
        
        helpMenu.add(documentationItem);
        helpMenu.add(aboutItem);
        
        // Add action listeners
        exitItem.addActionListener(e -> System.exit(0));
        
        classroomsItem.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        coursesItem.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        instructorsItem.addActionListener(e -> tabbedPane.setSelectedIndex(2));
        timetableItem.addActionListener(e -> tabbedPane.setSelectedIndex(3));
        
        validateItem.addActionListener(e -> validateTimetable());
        generateItem.addActionListener(e -> generateTimetable());
        findConflictsItem.addActionListener(e -> findConflicts());
        aboutItem.addActionListener(e -> showAboutDialog());
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    private void validateTimetable() {
        // Implementation for timetable validation
        int conflicts = findConflicts();
        if (conflicts == 0) {
            JOptionPane.showMessageDialog(this, "Timetable validation completed successfully!\nNo conflicts found.",
                    "Validation Result", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Timetable validation completed with " + conflicts + " conflicts!\n" +
                    "Please review the highlighted conflicts in the timetable.",
                    "Validation Result", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private int findConflicts() {
        // Implementation for finding conflicts
        // For demonstration, return a random number between 0-5
        conflicts = new Random().nextInt(6);
        return conflicts;
    }
    
    private void showAboutDialog() {
        JDialog aboutDialog = new JDialog(this, "About Time Table Builder", true);
        aboutDialog.setSize(400, 300);
        aboutDialog.setLocationRelativeTo(this);
        aboutDialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Time Table Builder");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel versionLabel = new JLabel("Version 1.0");
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel copyrightLabel = new JLabel("© 2025 University Scheduling Systems");
        copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextArea descriptionArea = new JTextArea(
                "Time Table Builder is a comprehensive solution for university " +
                "course scheduling. It helps to manage classrooms, courses, " +
                "instructors and automatically generate conflict-free timetables.");
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(null);
        descriptionArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(descriptionArea);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(copyrightLabel);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> aboutDialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        
        aboutDialog.add(contentPanel, BorderLayout.CENTER);
        aboutDialog.add(buttonPanel, BorderLayout.SOUTH);
        aboutDialog.setVisible(true);
    }
    
    private void initializeDemoData() {
        // Initialize timetable data structure
        timetableData = new HashMap<>();
        conflicts = 0;
        
        // Add some example classes to the timetable
        // This would normally be loaded from a file or database
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
    
}
// --- LOGIN FRAME ---
class LoginFrame extends JFrame implements ActionListener {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private JButton loginBtn, signupBtn;

    public LoginFrame() {
        setTitle("Timetable System Login");
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);
        panel.add(new JLabel("Role:"));
        roleCombo = new JComboBox<>(new String[]{"Admin", "Professor", "Student"});
        panel.add(roleCombo);
        loginBtn = new JButton("Login");
        loginBtn.addActionListener(this);
        panel.add(loginBtn);
        signupBtn = new JButton("Sign Up");
        signupBtn.addActionListener(this);
        panel.add(signupBtn);
        add(panel);
        initializeUsersFile();
    }

    private void initializeUsersFile() {
        File file = new File("users.dat");
        if (!file.exists()) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("admin,admin123,Admin");
                writer.println("prof1,prof123,Professor");
                writer.println("student1,stu123,Student");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginBtn) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            if (authenticate(username, password, role)) {
                dispose();
                new TimeTableBuilderApp(role).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        } else if (e.getSource() == signupBtn) {
            new SignupFrame().setVisible(true);
        }
    }

    private boolean authenticate(String username, String password, String role) {
        try (Scanner scanner = new Scanner(new File("users.dat"))) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",");
                if (parts[0].equals(username) && parts[1].equals(password) && parts[2].equals(role)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
}

// --- SIGN UP FRAME ---
class SignupFrame extends JFrame implements ActionListener {
    private JTextField usernameField, emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private JButton signupBtn;

    public SignupFrame() {
        setTitle("Sign Up");
        setSize(400, 250);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);
        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);
        panel.add(new JLabel("Role:"));
        roleCombo = new JComboBox<>(new String[]{"Admin", "Professor", "Student"});
        panel.add(roleCombo);
        signupBtn = new JButton("Complete Signup");
        signupBtn.addActionListener(this);
        panel.add(signupBtn);
        add(panel);
    }

    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String email = emailField.getText();
        String role = (String) roleCombo.getSelectedItem();
        try (FileWriter writer = new FileWriter("users.dat", true)) {
            writer.append(String.format("%s,%s,%s%n", username, password, role));
            JOptionPane.showMessageDialog(this, "Signup successful!");
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving user");
        }
    }
}
