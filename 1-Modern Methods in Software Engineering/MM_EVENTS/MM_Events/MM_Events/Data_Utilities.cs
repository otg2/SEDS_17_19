
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Data.SqlClient;
using System.IO;
using System.Linq;
using System.Net.Mail;
using System.Net.Mime;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using Telerik.Web.UI;

/// <summary>
/// Summary description for Data_Utilities
/// </summary>
public static class Data_Utilities
{
    


    // Same as above, but works for SQL databases
    public static DataTable getSQLDataByQuery(string aQuery)
    {
        DataTable _dataView = new DataTable();
        ConnectionStringSettings _connString = ConfigurationManager.ConnectionStrings["SqlDataBase"];
        using (SqlConnection con = new SqlConnection(_connString.ToString()))
        {
            con.Open();
            SqlCommand _command = new SqlCommand();
            _command.Connection = con;
            _command.CommandText = aQuery;
            _command.ExecuteNonQuery();

            SqlDataAdapter sda = new SqlDataAdapter(_command);
            sda.Fill(_dataView);
            con.Close();
        }
        return _dataView;
    }

    // Same as above, but works for SQL databases
    public static DataTable getSQLDataByQuery_Parameters(string aQuery, List<String[]> aParameters)
    {
        DataTable _dataView = new DataTable();
        ConnectionStringSettings _connString = ConfigurationManager.ConnectionStrings["SqlDataBase"];
        using (SqlConnection con = new SqlConnection(_connString.ToString()))
        {
            con.Open();
            SqlCommand _command = new SqlCommand();
            _command.Connection = con;
            _command.CommandText = aQuery;
            for (int i = 0; i < aParameters.Count; i++) _command.Parameters.AddWithValue(aParameters[i][0], aParameters[i][1]);

            SqlDataAdapter sda = new SqlDataAdapter(_command);
            sda.Fill(_dataView);
            con.Close();
        }
        return _dataView;
    }

    public static void ModifyDataBase_Parameters(string aQuery, List<String[]> aParameters)
    {
        ConnectionStringSettings _connString = ConfigurationManager.ConnectionStrings["SqlDataBase"];
        using (SqlConnection con = new SqlConnection(_connString.ToString()))
        {
            con.Open();
            SqlCommand _command = new SqlCommand();
            _command.Connection = con;

            // Set values
            for (int i = 0; i < aParameters.Count; i++) _command.Parameters.AddWithValue(aParameters[i][0], aParameters[i][1]);

            _command.CommandText = aQuery;
            _command.ExecuteNonQuery();


        }
    }

    // Used to modify the body content of emails.
    // Turns all image snapshots to alternitive view, so they can be sent in email
    // If the image is c/p from computer (which the control cannot handle) a "error" image is used

    //Used to fetch a task for a given id
    public static DataRow GetRequest(int id)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", id.ToString()}
        };

        DataTable _table = getSQLDataByQuery_Parameters("select * from Request where ReqId = @ReqId", _parameters);

        return _table.Rows[0];
    }

    public static void SetResponsibleForRequest(int requestId, string sendTo)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() },
            new string[] { "@ReqResp", sendTo}
        };

        ModifyDataBase_Parameters("UPDATE Request SET ReqResp = @ReqResp where ReqId = @ReqId", _parameters);
    }

    public static void setEventStatusToAccepted(int requestId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() }
        };

        var query = "UPDATE Events ";
        query += "SET EventStatus = 'APPROVED' ";
        query += "WHERE EventId = (SELECT ReqTaskId ";
        query += "FROM Request ";
        query += "WHERE ReqId = @ReqId) ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    public static void setEventRequestToClosed(int requestId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() }
        };

        var query = "UPDATE Request ";
        query += "SET ReqStatus = 'CLOSED' ";
        query += "WHERE ReqId = @ReqId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    public static void SaveFinancialComment(int requestId, string financialComment)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() },
            new string[] { "@FinancialComment", financialComment }
        };

        var query = "UPDATE Events ";
        query += "SET FinancialComment = @FinancialComment ";
        query += "WHERE EventId = (SELECT ReqTaskId ";
        query += "FROM Request ";
        query += "WHERE ReqId = @ReqId) ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    public static void SetEventStatusToReady(int requestId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() },
        };

        var query = "UPDATE Request ";
        query += "SET ReqStatus = 'READY' ";
        query += "WHERE ReqId = @ReqId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    public static void SaveFinancialBudget(int requestId, decimal budget)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() },
            new string[] { "@FinancialBudget", budget.ToString() }
        };

        var query = "UPDATE Events ";
        query += "SET EventBudget = @FinancialBudget ";
        query += "WHERE EventId = (SELECT ReqTaskId ";
        query += "FROM Request ";
        query += "WHERE ReqId = @ReqId) ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    public static void CancelEvent(int requestId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() }
        };

        var query = "UPDATE Events ";
        query += "SET EventStatus = 'CLOSED' ";
        query += "WHERE EventId = (SELECT ReqTaskId ";
        query += "FROM Request ";
        query += "WHERE ReqId = @ReqId) ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    public static DataRow GetTask(int taskId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskID", taskId.ToString()}
        };

        DataTable _table = getSQLDataByQuery_Parameters("SELECT * FROM Task WHERE TaskId = @TaskId", _parameters);

        return _table.Rows[0];
    }

    public static void SetTaskStatus(int taskId, string nextStatus)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskId", taskId.ToString() },
            new string[] { "@TaskStatus", nextStatus}
        };

        var query = "UPDATE Task ";
        query += "SET TaskStatus = @TaskStatus ";
        query += "WHERE TaskId = @TaskId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    public static void CloseTask(int taskId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskId", taskId.ToString() },
        };

        var query = "UPDATE Task ";
        query += "SET TaskStatus = 'CLOSED' ";
        query += "WHERE TaskId = @TaskId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    internal static void SetTaskExtraBudget(int taskId, decimal budget)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskId", taskId.ToString() },
            new string[] { "@TaskExtraBudget", budget.ToString() }
        };

        var query = "UPDATE Task ";
        query += "SET TaskExtraBudget = @TaskExtraBudget ";
        query += "WHERE TaskId = @TaskId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    internal static void SetTaskExtraComment(int taskId, string comment)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskId", taskId.ToString() },
            new string[] { "@TaskExtraComment", comment }
        };

        var query = "UPDATE Task ";
        query += "SET TaskExtraComment = @TaskExtraComment ";
        query += "WHERE TaskId = @TaskId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    internal static void SetTaskResponsible(int taskId, string responsible)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskId", taskId.ToString() },
            new string[] { "@TaskTeam", responsible }
        };

        var query = "UPDATE Task ";
        query += "SET TaskTeam = @TaskTeam ";
        query += "WHERE TaskId = @TaskId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    internal static void SetTaskBudget(int taskId, decimal budget)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskId", taskId.ToString() },
            new string[] { "@TaskBudget", budget.ToString() }
        };

        var query = "UPDATE Task ";
        query += "SET TaskBudget = @TaskBudget ";
        query += "WHERE TaskId = @TaskId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    internal static DataRow GetTaskForRequest(int requestId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@RequestId", requestId.ToString()}
        };

        var query = "SELECT * ";
        query += "FROM Task ";
        query += "WHERE TaskId = (SELECT ReqTaskId ";
        query += "FROM Request ";
        query += "WHERE ReqId = @RequestId)";

        DataTable _table = getSQLDataByQuery_Parameters(query, _parameters);

        return _table.Rows[0];
    }

    internal static void SetRequestStatus(int requestId, string status)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@ReqId", requestId.ToString() },
            new string[] { "@ReqStatus", status }
        };

        var query = "UPDATE Request ";
        query += "SET ReqStatus = @ReqStatus ";
        query += "WHERE ReqId = @ReqId ";

        ModifyDataBase_Parameters(query, _parameters);
    }

    internal static string GetUserRole(string user)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@Username", user}
        };

        var query = "SELECT UserRole ";
        query += "FROM Users ";
        query += "WHERE Username = @Username ";

        DataTable _table = getSQLDataByQuery_Parameters(query, _parameters);

        return _table.Rows[0]["UserRole"] as string;
    }

    internal static void SetTaskStatusToFinished(int taskId)
    {
        var _parameters = new List<string[]>
        {
            new string[] { "@TaskId", taskId.ToString() },
        };

        var query = "UPDATE Task ";
        query += "SET TaskStatus = 'FINSHED' ";
        query += "WHERE TaskId = @TaskId ";

        ModifyDataBase_Parameters(query, _parameters);
    }
}