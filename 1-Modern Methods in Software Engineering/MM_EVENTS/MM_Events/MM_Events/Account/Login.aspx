<%@ Page Title="Log in" Language="C#" MasterPageFile="~/Site.Master" AutoEventWireup="true" CodeBehind="Login.aspx.cs" Inherits="MM_Events.Account.Login" Async="true" %>
<%@ Register assembly="Telerik.Web.UI" namespace="Telerik.Web.UI" tagprefix="telerik" %>
<%@ Register assembly="Telerik.Web.UI.Skins" namespace="Telerik.Web.UI.Skins" tagprefix="telerikh" %>

<%@ Register Src="~/Account/OpenAuthProviders.ascx" TagPrefix="uc" TagName="OpenAuthProviders" %>

<asp:Content runat="server" ID="BodyContent" ContentPlaceHolderID="MainContent">
    <h2><%: Title %>.</h2>

    <div class="row">
        <div class="col-md-8">
            <section id="loginForm">
                <div class="form-horizontal">

                    <h4>Use a local account to log in.</h4>
                    <hr />
                      <asp:PlaceHolder runat="server" ID="ErrorMessage" Visible="false">
                        <p class="text-danger">
                            <asp:Literal runat="server" ID="FailureText" />
                        </p>
                    </asp:PlaceHolder>
                    <div class="form-group">
                        <asp:Label runat="server" AssociatedControlID="UserName" CssClass="col-md-2 control-label">User name</asp:Label>
                        <div class="col-md-10">
                            <asp:TextBox runat="server" ID="UserName" CssClass="form-control" />
                            <!--<asp:RequiredFieldValidator runat="server" ControlToValidate="UserName"
                                CssClass="text-danger" ErrorMessage="The user name field is required." /> -->
                        </div>
                    </div>
                    <div class="form-group" >
                        <asp:Label runat="server" AssociatedControlID="Password" CssClass="col-md-2 control-label">Password</asp:Label>
                        <div class="col-md-10">
                            <asp:TextBox runat="server" ID="Password" TextMode="Password" CssClass="form-control" Text="hallo1" />
                            <!--<asp:RequiredFieldValidator runat="server" ControlToValidate="Password" 
                                CssClass="text-danger" ErrorMessage="The password field is required." /> -->
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-md-offset-2 col-md-10">
                            <div class="checkbox">
                                <asp:CheckBox runat="server" ID="RememberMe" />
                                <asp:Label runat="server" AssociatedControlID="RememberMe">Remember me?</asp:Label>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-md-offset-2 col-md-10">
                            <asp:Button runat="server" OnClick="LogIn" Text="Log in" CssClass="btn btn-default" />
                        </div>
                    </div>
                </div>
                <p>
                    <asp:HyperLink runat="server" ID="RegisterHyperLink" ViewStateMode="Disabled">Register</asp:HyperLink>
                    if you don't have a local account.
                </p>
            </section>
        </div>

        <div class="col-md-4">
            <h4>Login with out custom made users - safe and easy!</h4>
             <ul style="list-style:none">
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton8" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                    Text="ADAM - Audio Specialist" Value="ADAM" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton6" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="ALICE - Financial Manager" Value="ALICE" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton15" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="CHARLIE - Vice President" Value="CHARLIE" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton2" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="DANIEL - Chef" Value="DANIEL" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton7" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="JACK - Production Manager" Value="JACK" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton3" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="JANET - Senior Customer Service" Value="JANET" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton10" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="JULIA - Graphic designer" Value="JULIA" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton14" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="KATE - Senior Waitress" Value="KATE" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton12" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="MAGY - Decoration Architecht" Value="MAGY" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton1" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="MIKE - Administration" Value="MIKE" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                  <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton5" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                    Text="NATALIE - Service Department Manager" Value="NATALIE" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton13" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                    Text="NICOLAS - Network Engineer" Value="NICOLAS" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                 <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton4" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="SIMON - Human Resources" Value="SIMON" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton9" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="SAM - Customer Service" Value="SAM" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
                  <li style="margin:5px" >
                     <telerik:RadButton runat="server" ID="RadButton11" AutoPostBack="true" OnClick="Login_Telerik_Click" 
                     Text="TOBIAS - Photography" Value="TOBIAS" RenderMode="Lightweight"></telerik:RadButton>
                 </li>
            </ul>
            <section id="socialLoginForm">
                <uc:OpenAuthProviders runat="server" ID="OpenAuthLogin" />
            </section>
        </div>
    </div>
</asp:Content>
