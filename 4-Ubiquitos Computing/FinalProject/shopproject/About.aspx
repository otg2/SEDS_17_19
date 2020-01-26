<%@ Page Title="About" Language="C#" MasterPageFile="~/Site.Master" AutoEventWireup="true" CodeBehind="About.aspx.cs" Inherits="ShopUbiq.About" %>


<%@ Register assembly="Telerik.Web.UI" namespace="Telerik.Web.UI" tagprefix="telerik" %>
<%@ Register assembly="Telerik.Web.UI.Skins" namespace="Telerik.Web.UI.Skins" tagprefix="telerikh" %>



<asp:Content ID="BodyContent" ContentPlaceHolderID="MainContent" runat="server">


    <telerik:RadCodeBlock runat="server" ID="MainRadCodeBlock">

        <script type ="text/javascript" src ="../Scripts/storeMap.js?id=14" defer="defer"></script>
        

        <script type="text/javascript">

            var racks = [];
            var nodes = [];
            var items = [];

            window.onload = function ()
            {
                firstTime_Init = true;
                tryGeolocation();
            }


            function initDrawStore()
            {
                // GET ACTIVE RACKS
                racks = [];
                nodes = [];
                items = [];
                var rackObjInfo = $find("<%= RackHolder.ClientID %>").get_value().split('_');
                for (i = 0 ; i < rackObjInfo.length; i++) {
                    var rackInfo = rackObjInfo[i].split(';')
                    var rack =
                        {
                            x: rackInfo[1],
                            y: rackInfo[2],
                            width: rackInfo[3],
                            height: rackInfo[4],
                            node: rackInfo[0]

                        }
                    racks.push(rack)
                }
                // GET ACTIVE NODES
                var nodeObjInfo = $find("<%= NodeHolder.ClientID %>").get_value().split('_');
                for (i = 0 ; i < nodeObjInfo.length; i++) {
                    var nodeInfo = nodeObjInfo[i].split(';')
                    var node =
                        {
                            x: parseFloat(nodeInfo[0]),
                            y: parseFloat(nodeInfo[1]),
                            adjacencies: nodeInfo[2].split(',').map(Number)
                        }
                    nodes.push(node)
                }
                // GET ACTIVE ITEMS
                var cordObjInfo = $find("<%= CoordinatesHolder.ClientID %>").get_value().split('_');
                for (i = 0 ; i < cordObjInfo.length; i++) {
                    var itemInfo = cordObjInfo[i].split(';')
                    var item =
                        {
                            RACK_ID: itemInfo[0],
                            NAME: itemInfo[1],
                            PRICE: itemInfo[2]
                        }
                    items.push(item)
                }

                // Call orns function
                setupUpdateStore(nodes, racks, items);
            }

            var display;

            var apiGeolocationSuccess = function (position) {
                    showPosition(position);
            };

            var tryAPIGeolocation = function () {
                jQuery.post("https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyCgcL1ttiqTZW7F6OdlpHcoKTVfUGY4oE4", function (success) {
                    apiGeolocationSuccess({ coords: { latitude: success.location.lat, longitude: success.location.lng } });
                })
                    .fail(function (err) {
                        alert("API Geolocation error! \n\n" + err);
                    });
            };

            var browserGeolocationSuccess = function (position) {
                showPosition(position);
            };

            var browserGeolocationFail = function (error) {
                switch (error.code) {
                    case error.TIMEOUT:
                        alert("Browser geolocation error !\n\nTimeout.");
                        break;
                    case error.PERMISSION_DENIED:
                        if (error.message.indexOf("Only secure origins are allowed") == 0) {
                            tryAPIGeolocation();
                        }
                        break;
                    case error.POSITION_UNAVAILABLE:
                        // dirty hack for safari
                        if (error.message.indexOf("Origin does not have permission to use Geolocation service") == 0) {
                            tryAPIGeolocation();
                        } else {
                            alert("Browser geolocation error !\n\nPosition unavailable.");
                        }
                        break;
                }
            };

            var tryGeolocation = function () {
                if (navigator.geolocation) {
                    navigator.geolocation.getCurrentPosition(
                        browserGeolocationSuccess,
                        browserGeolocationFail,
                        { maximumAge: 50000, timeout: 20000, enableHighAccuracy: true });
                }
            };

            function showPosition(position) {
                if (firstTime_Init)
                {
                    firstTime_Init = false;
                    var ajaxManager = $find("<%= Store_AjaxManager.ClientID %>");
                    // Creates string cordToServer|lat;long
                    var cordString = "firstInit|" + position.coords.latitude + ";" + position.coords.longitude;
                    ajaxManager.ajaxRequest(cordString);
                }
                else
                {
                    if (display) {
                        alert(position.coords.latitude + ";" + position.coords.longitude)
                    }
                    else {
                        var ajaxManager = $find("<%= Store_AjaxManager.ClientID %>");
                        // Creates string cordToServer|lat;long
                        var cordString = "updateVeggies|" + position.coords.latitude + ";" + position.coords.longitude;
                        console.log(cordString);
                        ajaxManager.ajaxRequest(cordString);
                    }
                }
            }

            function updateVeggies()
            {
                display = false;
                tryGeolocation();
            }

            function displayVeggies()
            {
                display = true;
                tryGeolocation();
            }

            function processImage_Ajax(dataVal)
            {
                var ajaxManager = $find("<%= Store_AjaxManager.ClientID %>");
                //alert(dataVal.length)
                ajaxManager.ajaxRequest("ClassifyImage|"+dataVal);
            }

            function OpenUpsellWindow(sender, args)
            {
                var _window = $find('<%= UpsellCombos_Window.ClientID %>');
                _window.show();
            }

            function OpenMissingWindow(sender, args)
            {
                var _window = $find('<%= MissingItems_Window.ClientID %>');
                _window.show();
            }

            function confirmMissingItem(sender, args)
            {
                var _item = sender.get_commandArgument()
                var ajaxManager = $find("<%= Store_AjaxManager.ClientID %>");

                ajaxManager.ajaxRequest("confirmMiss|" + _item);
            }

            function denyMissingItem(sender, args)
            {
                var _item = sender.get_commandArgument()
                var ajaxManager = $find("<%= Store_AjaxManager.ClientID %>");
                ajaxManager.ajaxRequest("denyMiss|" + _item);
            }

            function setSelectedView(sender, args) {
                var buttonClicked = sender.get_uniqueID()
                uniqID = buttonClicked.split('$').join("_");  //buttonClicked.replace(/$/g, "_");
                if (sender.get_checked()) {
                    $("#" + uniqID).addClass("clickedButtonClass");
                }
                else {
                    $("#" + uniqID).removeClass("clickedButtonClass");
                }
            }

        </script>
    </telerik:RadCodeBlock>


    <telerik:RadAjaxManager runat="server" ID="Store_AjaxManager" OnAjaxRequest="Store_AjaxManager_AjaxRequest" >
        <AjaxSettings>
            <telerik:AjaxSetting AjaxControlID="Store_AjaxManager">
                <UpdatedControls>
                    <telerik:AjaxUpdatedControl ControlID="Store_AjaxManager" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="NotifyLabel" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="debug" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="SelectUpdateItem" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>

                    <telerik:AjaxUpdatedControl ControlID="Display_MissingItems" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    
                    <telerik:AjaxUpdatedControl ControlID="SelectedStore_Id" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="SelectedStore_Name" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="DisplayStore" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    
                    <telerik:AjaxUpdatedControl ControlID="NodeHolder" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="RackHolder" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="CoordinatesHolder" UpdatePanelRenderMode="Inline"/>
                    
                </UpdatedControls>
            </telerik:AjaxSetting>
            <telerik:AjaxSetting AjaxControlID="UpdateItem_Button">
                <UpdatedControls>
                    <telerik:AjaxUpdatedControl ControlID="NotifyLabel" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                </UpdatedControls>
            </telerik:AjaxSetting>
            <telerik:AjaxSetting AjaxControlID="Combo_CreateButton">
                <UpdatedControls>
                    <telerik:AjaxUpdatedControl ControlID="Combo_Manage" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="ComboLabel" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                </UpdatedControls>
            </telerik:AjaxSetting>
            <telerik:AjaxSetting AjaxControlID="Combo_Manage">
                <UpdatedControls>
                    <telerik:AjaxUpdatedControl ControlID="Combo_Manage" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="ComboLabel" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="debug" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                </UpdatedControls>
            </telerik:AjaxSetting>
            
        </AjaxSettings>
    </telerik:RadAjaxManager>

    <telerik:RadAjaxLoadingPanel runat="server" ID="RadgridLoadingPanel" Skin="Windows7" Modal="false"></telerik:RadAjaxLoadingPanel>

    <telerik:RadWindowManager runat="server" ID="RadWindowManager">
        <Shortcuts>
            <telerik:WindowShortcut CommandName="close" Shortcut="esc" />
        </Shortcuts>
        <Windows>
            <telerik:RadWindow runat="server" ID="MissingItems_Window" Modal="true" CenterIfModal="true" Title="Missing items" >
                <ContentTemplate>
                    <div runat="server" id="Display_MissingItems">
                    </div>
                </ContentTemplate>
            </telerik:RadWindow>
            <telerik:RadWindow runat="server" ID="UpsellCombos_Window" Modal="true" CenterIfModal="true" 
                Title="Create upsell" Width="800" Height="600">
                <ContentTemplate>
                    <div style="background-color:bisque">
                        <h4 style="text-align:center">Saved combos</h4>
                        <telerik:RadGrid runat="server" ID="Combo_Manage"  OnNeedDataSource="Combo_Manage_NeedDataSource" 
                           RenderMode="Lightweight" OnDeleteCommand="Combo_Manage_DeleteCommand" Skin="Sunset">
                            <MasterTableView AutoGenerateColumns="False" DataKeyNames="COMBO_ID">
                            <Columns>
                              <telerik:GridButtonColumn CommandName="Delete" Text="Delete combo" UniqueName="DeleteColumn" />
                              <telerik:GridBoundColumn HeaderText="COMBO_ID" DataField="COMBO_ID" UniqueName="COMBO_ID" />

                              <telerik:GridBoundColumn HeaderText="Combo" DataField="PROD_NAME" UniqueName="PROD_NAME" />
                              <telerik:GridBoundColumn HeaderText="Date From" DataFormatString="{0:dd.MM.yyyy}" DataField="DATE_FROM" UniqueName="DATE_FROM" />
                              <telerik:GridBoundColumn HeaderText="Date To" DataFormatString="{0:dd.MM.yyyy}" DataField="DATE_TO" UniqueName="DATE_TO" />
                              <telerik:GridBoundColumn HeaderText="Time From" DataField="TIME_FROM" UniqueName="TIME_FROM" />
                              <telerik:GridBoundColumn HeaderText="Time to" DataField="TIME_TO" UniqueName="TIME_TO" />
                              <telerik:GridBoundColumn HeaderText="Active" DataField="ACTIVE" UniqueName="ACTIVE" />
                            </Columns>
                          </MasterTableView>
                        </telerik:RadGrid>
                    </div>
                    <div>
                        <h4 style="text-align:center">Create a new combo</h4>
                        <div id="selection">
                            <div style="text-align:center;margin:0 auto;">
                                <div class="fruitDiv">
                                <telerik:RadButton runat="server" ID ="Item_Apple" ButtonType="ToggleButton" AutoPostBack="false" Text="Apple"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" 
                                    >
                                    <Image ImageUrl="images/icons/apple.png" />
                                </telerik:RadButton>
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Avacado" ButtonType="ToggleButton" AutoPostBack="false" Text="Banana"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView"  >
                                    <Image ImageUrl="/images/icons/avacado.png" />
                                </telerik:RadButton>
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Banana" ButtonType="ToggleButton" AutoPostBack="false" Text="Pear"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                    <Image ImageUrl="images/icons/banana.png" />
                                </telerik:RadButton>   
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Kiwi" ButtonType="ToggleButton" AutoPostBack="false" Text="Pear"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                    <Image ImageUrl="images/icons/kiwi.png" />
                                </telerik:RadButton> 
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Lemon" ButtonType="ToggleButton" AutoPostBack="false" Text="Pear"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                    <Image ImageUrl="images/icons/lemon.png" />
                                </telerik:RadButton> 
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Lime" ButtonType="ToggleButton" AutoPostBack="false" Text="Avacado"
                                ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                <Image ImageUrl="images/icons/lime.png" />
                                </telerik:RadButton>
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Orange" ButtonType="ToggleButton" AutoPostBack="false" Text="Banana"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                    <Image ImageUrl="/images/icons/orange.png" />
                                </telerik:RadButton>
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Pear" ButtonType="ToggleButton" AutoPostBack="false" Text="Pear"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                    <Image ImageUrl="images/icons/pear.png" />
                                </telerik:RadButton>  
                                </div>
                                <div class="fruitDiv">
                 
                                <telerik:RadButton runat="server" ID ="Item_Pineapple" ButtonType="ToggleButton" AutoPostBack="false" Text="Pear"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                    <Image ImageUrl="images/icons/pineapple.png" />
                                </telerik:RadButton> 
                                </div>
                                <div class="fruitDiv">

                                <telerik:RadButton runat="server" ID ="Item_Strawberry" ButtonType="ToggleButton" AutoPostBack="false" Text="Strawberry"
                                    ToggleType="CheckBox" Checked="false" Width="200px" Height="200px" OnClientClicked="setSelectedView" >
                                    <Image ImageUrl="images/icons/strawberry.png" />
                                </telerik:RadButton> 
                                </div>
                            </div>
                        </div>
                        <div style="padding:20px;background-color:bisque">
                            <div style="text-align:center">
                                <telerik:RadLabel runat="server" ID="RadLabel1" Text="Date From" Width="150px"></telerik:RadLabel>

                                <telerik:RadDatePicker runat="server" ID="Combo_DateFrom" 
                                AutoPostBack="false"></telerik:RadDatePicker>
                            </div>
                            <div style="text-align:center">
                                <telerik:RadLabel runat="server" ID="Combo_DateTo_Label" Text="Date To  " Width="150px"></telerik:RadLabel>
                                <telerik:RadDatePicker runat="server" ID="Combo_DateTo" AutoPostBack="false" ></telerik:RadDatePicker>
                            </div>
                            <div style="text-align:center">
                                <telerik:RadLabel runat="server" ID="RadLabel2" Text="Time From" Width="150px"></telerik:RadLabel>
                                <telerik:RadTimePicker runat="server" ID="Combo_TimeFrom" AutoPostBack="false" ></telerik:RadTimePicker>
                            </div>
                            <div style="text-align:center">
                                <telerik:RadLabel runat="server" ID="RadLabel3" Text="Time To  " Width="150px"></telerik:RadLabel>
                                <telerik:RadTimePicker runat="server" ID="Combo_TimeTo" AutoPostBack="false" ></telerik:RadTimePicker>
                            </div>
                            <div style="text-align:center">
                                <telerik:RadButton runat="server" ID="Combo_Active" AutoPostBack="false" ButtonType="ToggleButton" ToggleType="CheckBox"
                                    text="Active" ></telerik:RadButton>
                            </div>
                        </div>
                    </div>
                    <div style="text-align:center">
                        <telerik:RadButton runat="server" ID="Combo_CreateButton" AutoPostBack="true" OnClick="Combo_CreateButton_Click"
                            text="Create Combo" RenderMode="Lightweight">
                            <Icon SecondaryIconCssClass="rbAdd" SecondaryIconRight="5px" />
                        </telerik:RadButton>
                        <div>
                            <telerik:RadLabel runat="server" ID="ComboLabel"></telerik:RadLabel>
                        </div>
                    </div>
                </ContentTemplate>
            </telerik:RadWindow>
            

            
        </Windows>
    </telerik:RadWindowManager>

    <h3 style="text-align:center" runat="server" id="DisplayStore">Update </h3>
    <div>
        <h3 style="text-align:center">1.Find category</h3>
        <div style="text-align:center">
            <telerik:RadDropDownList runat="server" ID="SelectUpdateItem" DataTextField="name" AutoPostBack="false" RenderMode="Lightweight">
            </telerik:RadDropDownList>
            <div runat="server" id="USE_TENSOR_DIV">
                <p> or </p>
                <input style="margin:0 auto; max-width:40%" type="file" accept="image/*" capture="camera" id="imgInput">
                <div>
                    <img id="imageDisplay" src="#" alt="your image" style="max-width:30%;max-height:30%" runat="server" />
                </div>
            </div>
            
        </div>
        <h3 style="text-align:center">2.Select rack location</h3>
        <div style="text-align:center">
            <div style="display:none">
                <!-- Hidden inputs used by server side -->
                <telerik:RadTextBox runat="server" ID="RackHolder" AutoPostBack="false"></telerik:RadTextBox>
                <telerik:RadTextBox runat="server" ID="NodeHolder" AutoPostBack="false"></telerik:RadTextBox>
                <telerik:RadTextBox runat="server" ID="CoordinatesHolder" AutoPostBack="false"></telerik:RadTextBox>
                <telerik:RadTextBox runat="server" ID="Selected_Rack" AutoPostBack="false"></telerik:RadTextBox>
                
                <telerik:RadTextBox runat="server" ID="SelectedStore_Id" AutoPostBack="false"></telerik:RadTextBox>
                <telerik:RadTextBox runat="server" ID="SelectedStore_Name" AutoPostBack="false"></telerik:RadTextBox>
            </div>
            <canvas id="myCanvas" width="500" height="500" style="border:solid 1px black;max-width:100%"></canvas>
        </div>
        <h3 style="text-align:center">3.Update the store</h3>

        <div style="text-align:center">
            <div>
                <telerik:RadLabel runat="server" ID="NotifyLabel"></telerik:RadLabel>
            </div>

            <telerik:RadButton runat="server" ID="UpdateItem_Button" Text="Update Store" AutoPostBack="true"
                            RenderMode="Lightweight" OnClick="UpdateItem_Button_Click" ></telerik:RadButton>
        </div>
         

    </div>
    <asp:Label ID="debug" runat="server" Text=""></asp:Label>
    <div   runat="server">
        <!-- Image processing -->

        <script type="text/javascript" defer="defer">

            var canvasObject = $("#myCanvas")

            canvasObject.click(function (jqEvent) {
                var coords = {
                    x: jqEvent.pageX - $(canvasObject).offset().left,
                    y: jqEvent.pageY - $(canvasObject).offset().top
                };
                console.log("X: " + coords.x + "_Y: " + coords.y);
                // TODO: Draw active rack as red/green/yellow 

                var _selectedRackId = getClosestRack(coords.x, coords.y);
                $find("<%= Selected_Rack.ClientID %>").set_value(_selectedRackId);

                // Redraw here
                rackSelected(_selectedRackId);
                //drawStore();

            });

            function getClosestRack(clickX, clickY) {
                var closestRack = 0;
                var distance = 1000;
                var newDistance;
                //console.log("hey");
                for (var i = 0; i < racks.length; i++) {
                    var rackX = parseInt(racks[i].x);
                    //console.log("clickX: " + typeof(clickX));
                    //console.log("parsed clickX : " + typeof(parseInt(clickX)));
                    var rackY = parseInt(racks[i].y);
                    var width = parseInt(racks[i].width);
                    var height = parseInt(racks[i].height);
                    newDistance = distanceToRack(rackX, rackY, width, height, clickX, clickY);
                    if (newDistance >= 0) {
                        if (newDistance === 0) {
                            return i;
                        }
                        else if (newDistance < distance) {
                            distance = newDistance;
                            closestRack = i;
                        }
                    }
                }
                //console.log("Distance: " + distance);
                return closestRack;
            }

            function distanceToRack(rackX, rackY, width, height, clickX, clickY) {
                //Left of rack
                if (clickX < rackX) {
                    //Above rack
                    if (clickY < rackY) {
                        return calcDistance(clickX, clickY, rackX, rackY);
                    }
                        //Beneath rack
                    else if (clickY > rackY + height) {
                        return calcDistance(clickX, clickY, rackX, rackY + height);
                    }
                        // Inside Y wise
                    else {
                        return Math.abs(rackX - clickX);
                    }
                }
                    //Right of rack
                else if (clickX > (rackX + width)) {
                    //Above rack
                    if (clickY < rackY) {
                        return calcDistance(clickX, clickY, rackX + width, rackY);
                    }
                        //Beneath rack
                    else if (clickY > rackY + height) {
                        return calcDistance(clickX, clickY, rackX + width, rackY + height);
                    }
                        // Inside Y wise
                    else {
                        return Math.abs(rackX - clickX);
                    }
                }
                    //Inside rack X wise
                else {
                    //Above rack
                    if (clickY < rackY) {
                        return Math.abs(rackY - clickY);
                    }
                        //Beneath rack
                    else if (clickY > rackY + height) {
                        return Math.abs(rackY - clickY);
                    }
                        //Inside rack
                    else {
                        console.log("rackX: " + rackX + ", rackY: " + rackY + ", clickX: " + clickX + ", clickY: " + clickY);
                        return 0;
                    }
                }
            }

            function calcDistance(x1, y1, x2, y2) {
                var xSide = Math.abs(x1 - x2);
                var ySide = Math.abs(y1 - y2);
                return parseInt(Math.sqrt(xSide * xSide + ySide * ySide));
            }

            // Read file
            function readURL(input, imgDisp) {
                if (input.files && input.files[0]) {
                    var reader = new FileReader();

                    reader.onload = function (e) {
                        // For display
                        imgDisp.attr('src', e.target.result);
                        // Send to server for preprocess
                        processImage_Ajax(e.target.result);

                    }
                    reader.readAsDataURL(input.files[0]);
                }
            }

            $("#imgInput").change(function () {
                var imageObj = $('#' + '<%= imageDisplay.ClientID %>')
                readURL(this, imageObj);
            });

        </script>
        <div class="row">
            <div class="col-md-4" style="text-align:center">
                <telerik:RadButton runat="server" ID="ReportedMissing_Button" Text="Items reported missing" AutoPostBack="false" OnClientClicked="OpenMissingWindow" 
                        RenderMode="Lightweight" Skin="Sunset">
                    <Icon SecondaryIconCssClass="rbConfig" SecondaryIconRight="5px" />
                </telerik:RadButton>
            </div>
            <div class="col-md-4" style="text-align:center">
                <telerik:RadButton runat="server" ID="UpsellButton_Combos" Text="Manage upsell combos" AutoPostBack="false" OnClientClicked="OpenUpsellWindow" 
                    RenderMode="Lightweight" Skin="Telerik" >
                    <Icon SecondaryIconCssClass="rbUpload" SecondaryIconRight="5px" />
                </telerik:RadButton>
            </div>
           <div class="col-md-4" style="text-align:center">
                
            </div>
        </div>

    </div>
    
</asp:Content>
