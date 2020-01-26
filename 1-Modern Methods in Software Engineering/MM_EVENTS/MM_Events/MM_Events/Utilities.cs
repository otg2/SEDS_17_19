
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Net.Mail;
using System.Text;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using Telerik.Web.UI;

/// <summary>
/// Summary description for Utilities
/// </summary>
public static class Utilities
{
    /* ------ SETTINGS METHODS -------*/
    // Changes a radgrid to a predefined settings
    public static void SetStandardGrid(RadGrid aGrid)
    {
        // Set filtering functions
        aGrid.AllowPaging = true;
        aGrid.PageSize = 25;
        //aGrid.PagerStyle.PageSizeControlType = // TODO : Set this
        aGrid.AllowSorting = true;
        aGrid.AllowFilteringByColumn = false;
        aGrid.MasterTableView.ToolTip = "Double click to open (or select and press enter)";

        // Set grid visualization
        aGrid.SelectedItemStyle.ForeColor = System.Drawing.ColorTranslator.FromHtml("Black");
        aGrid.ShowGroupPanel = false;

        // Set Client Settings
        aGrid.ClientSettings.AllowDragToGroup = false;
        aGrid.ClientSettings.AllowColumnsReorder = false;
        aGrid.ClientSettings.EnableRowHoverStyle = true;
        aGrid.ClientSettings.EnableAlternatingItems = false;
        aGrid.ClientSettings.AllowKeyboardNavigation = true;
        aGrid.ClientSettings.Selecting.AllowRowSelect = true;
        aGrid.ClientSettings.Scrolling.AllowScroll = true;
        aGrid.ClientSettings.Scrolling.UseStaticHeaders = true;
        aGrid.ClientSettings.Scrolling.EnableVirtualScrollPaging = false;
        aGrid.ClientSettings.Resizing.AllowColumnResize = true;
    }


    // Sets textbox to readonly and display mode
    public static void DisableTextBox(RadTextBox aBox)
    {
        aBox.ReadOnly = true;
        aBox.ReadOnlyStyle.BackColor = System.Drawing.ColorTranslator.FromHtml("Transparent");
        aBox.ReadOnlyStyle.BorderStyle = System.Web.UI.WebControls.BorderStyle.None;
        aBox.Font.Italic = true;
        //aBox.HoveredStyle.st["cursor"] = "pointer";
        aBox.HoveredStyle.BorderStyle = System.Web.UI.WebControls.BorderStyle.None;
        aBox.HoveredStyle.BackColor = System.Drawing.ColorTranslator.FromHtml("Transparent");
    }

   
    /* ------ DEBUGGING METHODS -------*/
   
}