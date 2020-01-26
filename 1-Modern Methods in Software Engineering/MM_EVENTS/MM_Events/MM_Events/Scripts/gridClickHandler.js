function openModalWindow(aReference, aWidth, aHeight)
{
    var _workboardWidth = $(window).width() * aWidth;
    var _workboardHeight = $(window).height() * aHeight;

    var oWnd = aReference;
    oWnd.setSize(_workboardWidth, _workboardHeight);
    oWnd.set_modal(true);
    oWnd.set_centerIfModal(true);
    oWnd.set_animation(Telerik.Web.UI.WindowAnimation.Slide);
    oWnd.set_animationDuration(500);
    oWnd.show();
    setWindowBehavior(oWnd);
    setTimeout(function () {
        // if (_currentInvoice_isExternalView) _externalInvoiceWindow.setActive(true);
        var overlay = $telerik.getElementByClassName(document, "TelerikModalOverlay");
        overlay.onclick = function () {
            oWnd.close();
            var scrollAmount = window.pageYOffset;
            setTimeout(function () { window.scrollTo(0, scrollAmount); }, 10);
        }
    }, 510);
}



// Sets the input in a new radwindow by the given value of the objects controller
function setEmptyInput(aControl, anEmptyArray)
{
    for (var i = 0 ; i < aControl.length; i++) {
        // Define type of input.
        // If type has no control, it's value is set as empty. Otherwise, the value is set the same as its controller
        if (aControl[i].type == "Dropdown") {
            if (aControl[i].controller == null) aControl[i].obj.get_items().getItem(0).select();
            else {
                var index = aControl[i].controller.get_selectedItem().get_index();
                aControl[i].obj.get_items().getItem(index).select();
            }
        }
        else if (aControl[i].type == "Textbox") {
            if (aControl[i].controller == null) aControl[i].obj.set_value(anEmptyArray[i]);
            else aControl[i].obj.set_value(aControl[i].controller.get_text());
        }
        else if (aControl[i].type == "Checkbox") {
            if (aControl[i].controller == null) aControl[i].obj.set_checked(false);
            // TODO : Add checkbox function when that is used
        }
        else if (aControl[i].type == "SearchBox") {
            //aControl[i].obj.set_text(aValue[i].trim());
        }
        else if (aControl[i].type == "DatePicker") {
            aControl[i].obj.set_selectedDate(null);
        }
        else aControl[i].obj.set_value("");
    }
}

function testHello()
{
    console.log("hello - JS compile fixed on IIS");
}

// Sets the input in a new radwindow by the given data
function setInput(aControl, aValue) {
    for (var i = 0 ; i < aControl.length; i++) {

        // Define type of input
        // Set its value by the row
        if (aControl[i].type == "Dropdown")
        {
            var dropDownItems = aControl[i].obj.get_items();
            var _selectedIndex = 0;
            for (var j = 0; j < dropDownItems.get_count() ; j++) {
                if (dropDownItems.getItem(j).get_text() == aValue[i]) {
                    _selectedIndex = j;
                    //dropDownItems.getItem(j).select();
                    break;
                }
            }
            dropDownItems.getItem(_selectedIndex).select();
        }
        else if (aControl[i].type == "Textbox") {
            // TELERIK interpreters null values from oracle as an empty space. Thus, if null values marked as the html code &nbsp (empty space) is found, simply replace it.
            aControl[i].obj.set_value(aValue[i].trim());
        }
        else if (aControl[i].type == "Checkbox") {
            var checked = false;
            if (aValue[i] == 1) checked = true;
            aControl[i].obj.set_checked(checked);
        }
        else if (aControl[i].type == "SearchBox") {
            var nameSearchBox = aControl[i].obj;
            var nameSearchBoxInput = nameSearchBox.get_inputElement();
            nameSearchBoxInput.focus();
            nameSearchBoxInput.value = aValue[i].trim();
            nameSearchBox.repaint();
        }
        else if (aControl[i].type == "DatePicker") {
            
            var _splitValue = aValue[i].split('.');
            var _date = new Date(_splitValue[2],_splitValue[1]-1,_splitValue[0]);
            aControl[i].obj.set_selectedDate(_date);
        }
    }
}

// Creates a new type of data object. Takes in the object itself, its type ( TODO : Find a way to distinguish its type by obj.get_type() or something) 
// and its controller. The controller is a control from a table filter. If user has searched for a input in value and wants to create a new data element, the starting value
// of the object will be set the same as the controller.
function controlObject(object, type, controller) {
    this.obj = object;
    this.type = type;
    this.controller = controller;
}

// Returns the id of the grid. 
// For now it only gets the last char of an id since all controls are defined by a numerator in the end. 
// Thus, this only covers ID's from 0-9 atm,
function windowLayout(gridSender)
{
    var layoutType = gridSender.get_id();
    return layoutType.charAt(layoutType.length - 1);
}

// Adds a function so user can click out of focus box to close it. Not generally supported by Telerik.
function overlayCloseHandler(sender)
{
    var overlay = $telerik.getElementByClassName(document, "TelerikModalOverlay");
    overlay.onclick = function ()
    {
        sender.close();
    }
}

function setWindowBehavior(aWindow)
{
    aWindow.set_behaviors(Telerik.Web.UI.WindowBehaviors.Move + Telerik.Web.UI.WindowBehaviors.Close
        + Telerik.Web.UI.WindowBehaviors.Resize + Telerik.Web.UI.WindowBehaviors.Maximize);
}

// Uses masterview to get info from every datacell in selected row
// Returns an array of values seated by their position in the grid
function rowInfo(aTableView)
{
    // Fetch selected row (only allow one row)
    var selectedItems = aTableView.get_selectedItems();
    var row = selectedItems[0];

    // Get all columns
    var columns = aTableView.get_columns();
    var resultArray = [];
    for (var i = 0; i < columns.length; i++) {
        // Get value from each column in row. 
        var category = columns[i].get_uniqueName();
        var cellValue = $(row.get_cell(category)).text();

        resultArray.push(cellValue);
    }
    // Return row info as array
    return resultArray;
}

function getColumnByUniqueName(aRadgridSender, anUniqueName)
{
    // Fetch selected row (only allow one row)
    var tableView = aRadgridSender.get_masterTableView();
    var selectedItems = tableView.get_selectedItems();
    var row = selectedItems[0];

    // Get all columns
    var columns = tableView.get_columns();
    //console.log(columns)
    for (var i = 0; i < columns.length; i++) {
        // Get value from each column in row. 
        var category = columns[i].get_uniqueName().toUpperCase();
        if (category == anUniqueName.toUpperCase())
        {
            return $(row.get_cell(category)).text();
        }
    }
    // if nothing is found, return NULL
    return null;
}

// Creates a new empty array when generating new keys
function emptyStringArray(length)
{
    var arr = [];
    for (var i = 0; i < length; ++i) { arr.push(''); }
    return arr;
}

// Loads and appends new form inside pre-displayed radwindow for specific actions/functions
function loadForm(divID, buttonPanel)
{
    $("#KeyButtonDiv").children().appendTo($("#hide"));
    $("#KeyButtonDiv").append($(buttonPanel));
    $("#" + divID).append($("#KeyButtonDiv"));
    return $get(divID);
}

// Fires up a new radwindow with specific form and data.
function openRadWindow(form, insert, title, animation)
{
    // Close all windows that are currently active and create a new one with chosen info
    GetRadWindowManager().closeAll();
    var editWindow = GetRadWindowManager().open(null, null, insert); 

    var keyInputs = ($("#" + form).children(":visible").length) - 1; // Skip the last child - the div for the buttons
    editWindow.setSize(134 * keyInputs, 200);
    editWindow.set_modal(true);

    if (!animation)
    {
        editWindow.set_animation(Telerik.Web.UI.WindowAnimation.Slide);
        editWindow.set_animationDuration(320);
        editWindow.show();
    }
    
    overlayCloseHandler(editWindow);
    editWindow.set_title(title);
    editWindow.addShortcut("close", "esc");
    
    setWindowBehavior(editWindow);
}

// ADD AND EDIT FUNCTIONS

// Parameters 
// aSender - sender object
// aPrefex - prefix that finds the corresponding div
// aIndex - form number
// aTitle - title of element in Radwindow
// aControlArray - array of predetermined controlObjects
// Function loads the corresponding data from grid and populates into a form inside a radWindow. Buttons to modify the database follow.
function editElement(aSender, aPrefix, aIndex, aTitle, aControlArray, customAnimation)
{
    var divForm = aPrefix + aIndex;
    var divInsert = loadForm(divForm, '#EditButtons');

    var masterTableView = aSender.get_masterTableView();
    var keySelected = rowInfo(masterTableView);

    setInput(aControlArray, keySelected);

    openRadWindow(divForm, divInsert, aTitle, customAnimation);
}
// Parameters 
// aPrefex - prefix that finds the corresponding div
// aIndex - form number
// aTitle - title of element in Radwindow
// aControlArray - array of predetermined controlObjects
// Function loads the corresponding data from grid and populates into a form inside a radWindow. Buttons to modify the database follow.
function newElement(aPrefix, aIndex, aTitle, aControlArray, customAnimation)
{
    var divForm = aPrefix + aIndex;
    var divInsert = loadForm(divForm, '#NewButtons');

    var cleanArray = emptyStringArray(divInsert.children.length);
    
    setEmptyInput(aControlArray, cleanArray);

    openRadWindow(divForm, divInsert, aTitle, customAnimation);
}
