import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class TextEditor1 extends Application {

	public static void main(String[] args) {
		launch();
	}
	// -----------------------------------------------

	private BorderPane root;
	private FileChooser fileChooser;	// The dialog box used to open and save files.
	private TabPane tabPane;			// The pane that holds all the tabs.	
	
	public void start(Stage stage) throws Exception {
		tabPane = new TabPane();	
		tabPane.setTabDragPolicy(TabDragPolicy.REORDER);
		tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		// Create a new document.
		doNew();
		root = new BorderPane();
		root.setTop(createMenus(stage));
		root.setCenter(tabPane);
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.setTitle("Text Editor");
		stage.setResizable(true);
		stage.centerOnScreen();
		Rectangle2D screen = Screen.getPrimary().getBounds();
		double width = screen.getWidth() / 4;
		double height = screen.getHeight() / 2;
		stage.setWidth(width);
		stage.setHeight(height);
		stage.setMinWidth(width);
		stage.setMinHeight(height / 4);
		stage.show();		
	}
	
	/**
	 * Gets the currently selected tab.
	 * @return the currently selected document.
	 */
	private int getSelectedTabByIndex() {
		return tabPane.getSelectionModel().getSelectedIndex();
	}
	
	/**
	 * 
	 * @return
	 */
	private Tab getSelectedTabByTab() {
		return tabPane.getSelectionModel().getSelectedItem();
	}
	
	// ------------- FILE METHODS -------------- //
	
	boolean unsavedChanges;
	
	/**
	 * Creates a new document and the tab associated with that document.
	 */
	private void doNew() {	
		int unsavedDocumentNumber = 1;
		String documentName = "Unsaved document ";
		
		// TODO: Doesn't handle numbers properly 
		for(int i = 0; i < tabPane.getTabs().size(); i++) {	
			if(tabPane.getTabs().get(i).getText().equals("Unsaved document " + unsavedDocumentNumber) ) 
				unsavedDocumentNumber++;			
		}		
		Tab tab = new Tab();
		tab.setText(documentName + unsavedDocumentNumber);
		
		// Adds a * to the tab if it's been modified.
		TextArea area = new TextArea();
		int docNumber = unsavedDocumentNumber;
		area.setOnKeyPressed(evt -> tab.setText("*" + documentName + docNumber));
		tab.setContent(area);
		
		
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);		
	}
	
	/**
	 * Opens a file specified by the user via a file chooser.
	 * @param stage
	 * @throws IOException
	 */
	private void doOpenFile(Stage stage) throws IOException {
		fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.setInitialDirectory(new File("/home/"));
		File file = fileChooser.showOpenDialog(stage);
		
		// Check to see if file is already open.
		Tab open;
		for(int i = 0; i < tabPane.getTabs().size(); i++) {		
			open = tabPane.getTabs().get(i);
			String fileName = open.getText();
			System.out.println(fileName);

			if( fileName.equals(file.getName()) ) {
				tabPane.getSelectionModel().select(open);
				return;
			}
		}		
		// File isn't already open if this is being executed.			
		TextArea textArea = new TextArea();
		textArea.setText(getContentFromFile(file));		
		Tab tab;
		// Checks if there is a selected unsaved tab and replaces it with the document to be opened.
		if(tabPane.getTabs().size() > 0 && getSelectedTabByTab().getText().contains("Unsaved") ) {
			tab = new Tab(file.getName(), textArea);
			int currentTab = getSelectedTabByIndex();
			tabPane.getTabs().set(currentTab, tab);
			tabPane.getSelectionModel().select(tab);
		}
		else {
			tab = new Tab(file.getName(), textArea);
			tabPane.getTabs().add(tab);
			tabPane.getSelectionModel().select(tab);
		}		
		
		// Saves the file path so it can be retrieved later.
		DocumentData data = new DocumentData();
		data.setFileLocation(file.getPath());
		tab.setUserData(data);
	}
	
	/**
	 * This is a utility method that reads a file and returns the content as a string.
	 * This is only meant for plain .txt files.
	 * @param file the file that is being read.
	 * @return a string with the contents of the file.
	 * @throws IOException if the file isn't found.
	 */
	private String getContentFromFile(File file) throws IOException {
		String content = "";
		Reader reader = new FileReader(file);
		BufferedReader buffreader = new BufferedReader(reader);
		ArrayList<String> lines = new ArrayList<String>();

		try {
			String line;
			while((line = buffreader.readLine()) != null) 
				lines.add(line);		
		}
		finally {
			reader.close();
		}		
		for(String line : lines) 
			content += line + "\n";
				
		return content;
	}
	
	/**
	 * This method is for saving when the document already has been saved >1 times
	 * and has a file path established.
	 * @param stage
	 * @throws FileNotFoundException
	 */
	private void doSave(Stage stage) throws FileNotFoundException {
		Tab tab = getSelectedTabByTab();
		if(tab.getText().contains("Unsaved")) {
			doSaveAs(stage);
		}
		else {
			PrintWriter writer = new PrintWriter(tab.getUserData().toString());
			writer.println(((TextArea) tab.getContent()).getText());
			writer.close();
		}
	}
	
	/**
	 * This method prompts the user with a file chooser so they can navigate the file structure and name the document 
	 * to be saved.
	 * @param stage where the file chooser shows up.
	 * @throws FileNotFoundException
	 */
	private void doSaveAs(Stage stage) throws FileNotFoundException {
		fileChooser = new FileChooser();
		fileChooser.setTitle("Save As..");
		fileChooser.setInitialDirectory(new File("/home/martin/Desktop"));
		File file = fileChooser.showSaveDialog(stage);
		
		Tab tab = getSelectedTabByTab();
		tab.setUserData(new String(file.getPath()));
		TextArea text = (TextArea) tab.getContent();
		PrintWriter writer = new PrintWriter(file);
		writer.println(text.getText());
		writer.close();
		
		tab.setText(file.getName());
	}
	
	/**
	 * Closes the currently selected tab.
	 */
	private void doClose() {
		Tab tab = getSelectedTabByTab();
		tabPane.getTabs().remove(tab);
	}
	
	// ------------------------------- EDIT METHODS ------------------------------------------- //
	
	private void doEditMethod(String method) {
		Tab tab = getSelectedTabByTab();
		TextArea area = (TextArea) tab.getContent();		
		switch(method) {	
			case "undo" -> area.undo();
			case "redo" -> area.redo();
			case "cut" -> area.cut();
			case "copy" -> area.copy();
			case "paste" -> area.paste();
			case "delete" -> area.deleteText(new IndexRange(area.getSelection()));
			case "selectAll" -> area.selectAll();		
		}
		
	}
	
	// ------------------------------- SEARCH METHODS ----------------------------------------- //
	
	
	private TextField searchInput, replaceInput;			// Input used to search for a word and place that word with another in the document.
	private Button next, prev, exit, replace, replaceAll;	// Buttons used in the search bar.
	private Label searchLabel;								// Label displaying search results.
	private ArrayList<Integer> pos = new ArrayList<Integer>(); // Holds the positions of the matching characters/string.
	private int inputLength;								   // The length of the input.
	private int searchCounter;								   // Counts what position in the array holding the positions of the characters/strings is in.
	private String replaceString;
	private enum Search {SEARCH, NEXT, PREV, REPLACE, REPLACEALL, EXIT};
	
	/**
	 * 
	 * @param search
	 * @param page
	 * @param evt
	 */
	private void doFindOrReplace(Search search, TextArea page, Event evt) {	
		searchLabel.setText("");
		Tab tab = getSelectedTabByTab();
		page = (TextArea) tab.getContent();
		
		switch(search) {
			case SEARCH -> {
				inputLength = searchInput.getCharacters().length();
				pos = doSearch(searchInput.getCharacters());
				searchCounter = 0;
				if(pos.size() == 0) {
					searchLabel.setText("No matches found.");
					return;
				}
				int index = pos.get(searchCounter);		
				page.selectRange(index, index + inputLength);
				searchLabel.setText((searchCounter + 1) + " of " + pos.size() + " matches found.");
				
			}
			case NEXT -> {
				if(searchCounter < pos.size() - 1)
					searchCounter++;		
				else
					searchCounter = 0;
				int index = pos.get(searchCounter);
				page.selectRange(index, index + inputLength);
				searchLabel.setText((searchCounter + 1) + " of " + pos.size() + " matches found.");
			}
			case PREV -> {
				if(searchCounter > 0)
					searchCounter--;		
				else
					searchCounter = pos.size() - 1;
				int index = pos.get(searchCounter);
				page.selectRange(index, index + inputLength);
				searchLabel.setText((searchCounter + 1) + " of " + pos.size() + " matches found.");
			}
			case REPLACE -> {
				replaceString = replaceInput.getText();
				//System.out.println(replaceString);
				int start = pos.get(searchCounter);
				int end = start + inputLength;
				page.replaceText(start, end, replaceString);	
				doFindOrReplace(Search.SEARCH, page, evt);
			}
			case REPLACEALL -> {
				replaceString = replaceInput.getText();
				for(int position : pos) {
					System.out.println("Position: " + position);
					int start = position;
					int end = start + inputLength;
					System.out.println("Start: " + start + " End: " + end);
					page.selectRange(start, end);
					page.replaceText(start, end, replaceString);
				}
			}
			case EXIT -> {
				// get button that triggered the action
			    Node n = (Node) evt.getSource();
			    // get node to remove
			    Node p = n.getParent().getParent();
			    // remove p from parent's child list
			    ((Pane) p.getParent()).getChildren().remove(p);
			}
		}
	}
	
	/**
	 * Searches the TextArea for the given CharSequence. It adds all the instances of the sequence to an ArrayList,
	 * and returns the ArrayList with the positions of each occurence.
	 * @param toSearch the characters to be searched.
	 */
	private ArrayList<Integer> doSearch(CharSequence toSearch) {
		ArrayList<Integer> positions = new ArrayList<Integer>();
		Tab tab = getSelectedTabByTab();
		TextArea page = (TextArea) tab.getContent();
		String searchString = toSearch.toString();
		String text = page.getText();
	
		if(text.contains(searchString)) {				
			int index = text.indexOf(searchString);
			while(index != -1) {		
				positions.add(index);
				System.out.println(index);
				int temp = index + 1;
				index = text.indexOf(searchString, temp);
			}
		}		
		return positions;
	}
	
	/**
	 * Creates the search bar and adds it to the bottom of the TextArea.
	 */
	private void addSearchBox() {	
		TilePane container = new TilePane();
		container.getChildren().add(makeSearchBox());
		container.getChildren().add(searchLabel);
		container.setStyle("-fx-background-color: lightgray; -fx-padding: 1 1 1 1;");
		root.setBottom(container);	
	}
	
	/**
	 * Creates the search and replace bar and adds it to the bottom of the TextArea.
	 */
	private void addSearchAndReplaceBox() {
		TilePane container = new TilePane();
		container.getChildren().add(makeSearchBox());
		container.getChildren().add(makeReplaceBox());
		container.getChildren().add(searchLabel);
		container.setStyle("-fx-background-color: lightgray; -fx-padding: 1 1 1 1;");
		root.setBottom(container);	
	}
	
	
	/**
	 * 
	 * @return
	 */
	private HBox makeSearchBox() {
		HBox searchBar;
		Tab tab = getSelectedTabByTab();
		TextArea page = (TextArea) tab.getContent();	
		
		searchInput = new TextField();
		searchInput.setPromptText("Search for:");
		searchInput.setPrefColumnCount(8);
		searchInput.setPrefWidth(250);
		searchInput.setOnAction(evt -> doFindOrReplace(Search.SEARCH, page, evt));
			
		next = new Button("Next");
		next.setPrefWidth(90);
		next.setOnAction(evt -> doFindOrReplace(Search.NEXT, page, evt));
		prev = new Button("Previous");
		prev.setPrefWidth(90);
		prev.setOnAction(evt -> doFindOrReplace(Search.PREV, page, evt));
		exit = new Button("x");
		exit.setOnAction(evt -> doFindOrReplace(Search.EXIT, page, evt));				
		searchBar = new HBox(searchInput, next, prev, exit);
		
		searchLabel = new Label();
				
		return searchBar;
	}
	
	/**
	 * 
	 * @return
	 */
	private HBox makeReplaceBox() {
		HBox replaceWithBar;
		
		Tab tab = getSelectedTabByTab();
		TextArea page = (TextArea) tab.getContent();
		
		replaceInput = new TextField();
		replaceInput.setPromptText("Replace with:");
		replaceInput.setPrefColumnCount(8);
		replaceInput.setPrefWidth(250);
		
		replace = new Button("Replace");
		replace.setPrefWidth(90);
		replace.setOnAction(evt -> doFindOrReplace(Search.REPLACE, page, evt) );
		replaceAll = new Button("ReplaceAll");
		replaceAll.setPrefWidth(90);
		replaceAll.setOnAction(evt -> doFindOrReplace(Search.REPLACEALL, page, evt) );

		replaceWithBar = new HBox(replaceInput, replace, replaceAll);	
		
		return replaceWithBar;
	}
	
	
	// -------------------------------------------------------------------------------------------------- //
	
	/**
	 * Utility method that creates the MenuBar and adds listeners to each item.
	 * @param stage the stage used for the "Open" and "Save As.." dialog boxes.
	 * @return the menu bar.
	 */
	private MenuBar createMenus(Stage stage) {
		MenuBar menubar;
		Menu menu;
		MenuItem item;
		
		menubar = new MenuBar(); // Create menu bar.
		
		// --------------- FILE MENU ----------------- //
		menu = new Menu("File");
		menubar.getMenus().add(menu);
		
		item = new MenuItem("New");
		item.setOnAction(evt -> doNew());	
		menu.getItems().add(item);
		
		item = new MenuItem("Open");
		item.setOnAction(evt -> {
			try {
				doOpenFile(stage);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		menu.getItems().add(item);
		
		// Separator. //
		menu.getItems().add(new SeparatorMenuItem());
		
		item = new MenuItem("Save");
		item.setOnAction(evt -> {
			try {
				doSave(stage);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		menu.getItems().add(item);
		
		item = new MenuItem("Save As..");
		item.setOnAction(evt -> {
			try {
				doSaveAs(stage);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		menu.getItems().add(item);
		
		// Separator. //
		menu.getItems().add(new SeparatorMenuItem());
		
		item = new MenuItem("Close");
		item.setOnAction(evt -> doClose());
		menu.getItems().add(item);
		
		item = new MenuItem("Quit");
		item.setOnAction(evt -> System.exit(0));
		menu.getItems().add(item);

		// --------------- EDIT MENU ----------------- //	
		menu = new Menu("Edit");
		menubar.getMenus().add(menu);

		item = new MenuItem("Undo");
		item.setOnAction(evt -> doEditMethod("undo"));
		menu.getItems().add(item);

		item = new MenuItem("Redo");
		item.setOnAction(evt -> doEditMethod("redo"));
		menu.getItems().add(item);
		
		// Separator. //
		menu.getItems().add(new SeparatorMenuItem());
		
		item = new MenuItem("Cut");
		item.setOnAction(evt -> doEditMethod("cut"));
		menu.getItems().add(item);
		
		item = new MenuItem("Copy");
		item.setOnAction(evt -> doEditMethod("copy"));
		menu.getItems().add(item);
		
		item = new MenuItem("Paste");
		item.setOnAction(evt -> doEditMethod("paste"));
		menu.getItems().add(item);
		
		item = new MenuItem("Delete");
		item.setOnAction(evt -> doEditMethod("delete"));
		menu.getItems().add(item);
		
		// Separator. //
		menu.getItems().add(new SeparatorMenuItem());
		
		item = new MenuItem("SelectAll");
		item.setOnAction(evt -> doEditMethod("selectAll"));
		menu.getItems().add(item);
		
		// --------------- VIEW MENU ----------------- //
		
		menu = new Menu("View");
		menubar.getMenus().add(menu);
		
		// --------------- SEARCH MENU ----------------- //
		
		menu = new Menu("Search");
		menubar.getMenus().add(menu);
		
		item = new MenuItem("Find");
		item.setOnAction(evt -> addSearchBox());
		menu.getItems().add(item);

		item = new MenuItem("Find Next");
		item.setOnAction(evt -> next.fire());
		menu.getItems().add(item);
		
		item = new MenuItem("Find Previous");
		item.setOnAction(evt -> prev.fire());
		menu.getItems().add(item);
		
		// Separator. //
		menu.getItems().add(new SeparatorMenuItem());
		
		item = new MenuItem("Replace");
		item.setOnAction(evt -> addSearchAndReplaceBox());
		menu.getItems().add(item);
		
		return menubar;
	} // end createMenus()

}


