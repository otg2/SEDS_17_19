<%@ Page Title="Home Page" Language="C#" MasterPageFile="~/Site.Master" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="ShopUbiq._Default" %>

<%@ Register assembly="Telerik.Web.UI" namespace="Telerik.Web.UI" tagprefix="telerik" %>
<%@ Register assembly="Telerik.Web.UI.Skins" namespace="Telerik.Web.UI.Skins" tagprefix="telerikh" %>

<asp:Content ID="BodyContent" ContentPlaceHolderID="MainContent" runat="server">


    <telerik:RadCodeBlock runat="server">
        <script type ="text/javascript" src ="../Scripts/storeMap.js?id=234" defer="defer"></script>


        <script type="text/javascript">

            var apiGeolocationSuccess = function (position) {
                console.log("api")
                showPosition(position)
                  //alert("API geolocation success!\n\nlat = " + position.coords.latitude + "\nlng = " + position.coords.longitude);
              };

            // 

            // AIzaSyDCa1LUe1vOczX1hO_iGYgyo8p_jYuGOPU
              var tryAPIGeolocation = function () {
                  jQuery.post("https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyCgcL1ttiqTZW7F6OdlpHcoKTVfUGY4oE4", function (success) {
                      apiGeolocationSuccess({ coords: { latitude: success.location.lat, longitude: success.location.lng } });
                  })
                      .fail(function (err) {
                          alert("API Geolocation error! \n\n" + err);
                      });
              };

              var browserGeolocationSuccess = function (position)
              {
                  console.log("browser")
                  showPosition(position)
                  //alert("Browser geolocation success!\n\nlat = " + position.coords.latitude + "\nlng = " + position.coords.longitude);
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

              function getLocation() {
                  tryGeolocation();
              }
              function showPosition(position) {
                  var ajaxManager = $find("<%= Veggies_AjaxManager.ClientID %>");
                  // Creates string cordToServer|lat;long
                  if (firstRequest)
                  {
                      var cordString = "findVeggies|" + position.coords.latitude + ";" + position.coords.longitude;
                  }
                  else
                  {
                      var cordString = "findAvailStore|" + position.coords.latitude + ";" + position.coords.longitude;
                  }
                  console.log(cordString);
                  ajaxManager.ajaxRequest(cordString);

              }

            var firstRequest = true;

            function findAvailStore()
            {
                firstRequest = false;
                clearCanvas();
                racks = [];
                nodes = [];
                items = [];
                getLocation();
            }

            function findVeggies(sender, args)
            {
                firstRequest = true;
                getLocation();
            }

            var racks = [];
            var nodes = [];
            var items = [];

            function initDrawStore()
            {
                // GET ACTIVE RACKS
                racks = [];
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
                nodes = [];
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
                items = [];
                var cordObjInfo = $find("<%= CoordinatesHolder.ClientID %>").get_value().split('_');
                for (i = 0 ; i < cordObjInfo.length; i++) {
                    var itemInfo = cordObjInfo[i].split(';')
                    var item =
                        {
                            RACK_ID: itemInfo[0],
                            NAME: itemInfo[1],
                            PRICE: itemInfo[2],
                            DRAW: itemInfo[3]
                        }
                    items.push(item)
                }

                var scrollToItem = $find("<%= TestLabel.ClientID %>");
                $("html, body").animate({ scrollTop: $("#myCanvas").offset().top - 150 }, 1000);

                // Call orns function
                setupStore(nodes, racks, items);
            }

            function setSelectedView(sender, args)
            {
                var buttonClicked = sender.get_uniqueID()
                uniqID = buttonClicked.split('$').join("_");  //buttonClicked.replace(/$/g, "_");
                if (sender.get_checked())
                {
                    $("#" + uniqID).addClass("clickedButtonClass");
                }
                else {
                    $("#" + uniqID).removeClass("clickedButtonClass");
                }
            }

        </script>
    </telerik:RadCodeBlock>
    <telerik:RadAjaxManager runat="server" ID="Veggies_AjaxManager" OnAjaxRequest="Veggies_AjaxManager_AjaxRequest" >
        <AjaxSettings>
            <telerik:AjaxSetting AjaxControlID="Veggies_AjaxManager">
                <UpdatedControls>
                    <telerik:AjaxUpdatedControl ControlID="Veggies_AjaxManager" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="NodeHolder" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="RackHolder" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="CoordinatesHolder" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="debug" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="ItemSelection" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="MissingItems_Button" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    
                    <telerik:AjaxUpdatedControl ControlID="ClosestAvailStore" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="LabelWarning" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    
                    <telerik:AjaxUpdatedControl ControlID="TestLabel" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                </UpdatedControls>
            </telerik:AjaxSetting>
            <telerik:AjaxSetting AjaxControlID="FindItems_Button">
                <UpdatedControls>
                    <telerik:AjaxUpdatedControl ControlID="FindItems_Button" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                    <telerik:AjaxUpdatedControl ControlID="TestLabel" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                </UpdatedControls>
            </telerik:AjaxSetting>
            <telerik:AjaxSetting AjaxControlID="MissingItems_Button">
                <UpdatedControls>
                    <telerik:AjaxUpdatedControl ControlID="NotificationLabel" LoadingPanelID="RadgridLoadingPanel" UpdatePanelRenderMode="Inline"/>
                </UpdatedControls>
            </telerik:AjaxSetting>
            
        </AjaxSettings>
    </telerik:RadAjaxManager>

    <telerik:RadAjaxLoadingPanel runat="server" ID="RadgridLoadingPanel" Skin="Telerik" Modal="false"></telerik:RadAjaxLoadingPanel>


    <div class="jumbotron">
        <div style="text-align:center;margin:0 auto;width:95%;">
            <asp:Label runat="server" ID="debug" Text=""></asp:Label>
            <h4>What do you need?</h4>
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
        <div style="text-align:center;clear:both;margin-top:10px">
            <telerik:RadButton runat="server" ID="FindItems_Button" Text="Find veggies!" AutoPostBack="false" Skin="Telerik"
                RenderMode="Lightweight" OnClientClicked="findVeggies" ></telerik:RadButton>
        </div>
        <div style="display:none">
            <telerik:RadTextBox runat="server" ID="RackHolder" AutoPostBack="false"></telerik:RadTextBox>
            <telerik:RadTextBox runat="server" ID="NodeHolder" AutoPostBack="false"></telerik:RadTextBox>
            <telerik:RadTextBox runat="server" ID="CoordinatesHolder" AutoPostBack="false"></telerik:RadTextBox>
        </div>
        
        <div style="text-align:center">
            <div>
                <telerik:RadLabel runat="server" ID="TestLabel" Text="Nearest store..." ></telerik:RadLabel>
            </div>
            <div>
                <div runat="server" id="LabelWarning"></div>
                <telerik:RadButton runat="server" ID="ClosestAvailStore" Visible="false" RenderMode="Lightweight"
                    Text="Show me the closest store that has all items" Skin="Telerik" OnClientClicked="findAvailStore"></telerik:RadButton>
            </div>
        </div>
        <div style="text-align:center">

            <canvas id="myCanvas" width="500" height="500" style="border:solid 1px black;max-width:100%"></canvas>
        </div>
    
    </div>

    <div class="row">
        <div class="col-md-4">
            <h2>Missing items?</h2>
            <p>Tell us which item is missing and out staff will check it out as soon as possible</p>
            <telerik:RadDropDownList runat="server" ID="ItemSelection" Visible="false"
                 DataTextField="name" AutoPostBack="false" RenderMode="Lightweight" ExpandDirection="Up">

            </telerik:RadDropDownList>
             <telerik:RadButton runat="server" ID="MissingItems_Button" Text="Let us know" AutoPostBack="true" Visible="false"
                    RenderMode="Lightweight" OnClick="MissingItems_Button_Click" ></telerik:RadButton>
            <telerik:RadLabel runat="server" ID="NotificationLabel"></telerik:RadLabel>
        </div>
    </div>

</asp:Content>
