<%@ Page Title="Map" Language="C#" MasterPageFile="~/Site.Master" AutoEventWireup="true" CodeBehind="Contact.aspx.cs" Inherits="ShopProject.Contact" %>

<asp:Content ID="BodyContent" ContentPlaceHolderID="MainContent" runat="server">
    <h2><%: Title %>.</h2>
        
    <Border
        BorderThickness="2"
        BorderBrush="Black"
        Background="LightGray"
        HorizontalAlignment="Left"
        VerticalAlignment="Top"
        Width="100"
        Height="100">
            <%--<canvas id="myCanvas" width="500" height="500" style="border:1px solid #000000;"></canvas>--%>
    </Border>

    <script type ="text/javascript" src ="../Scripts/storeMap.js?id=134"></script>

    <button onclick="runDjikstra(); return false;">Find Path</button> 
    <button onclick="clearPath(); return false;">Clear Path</button>  
    <button onclick="clearDestinations(); return false;">Clear Destinations</button> 
    Quantity (between 1 and 60):
    <input type="number" id="destination" name="quantity" min="1" max="60">
    <input type="submit" onclick ="updateDestinations(); return false;">


</asp:Content>
