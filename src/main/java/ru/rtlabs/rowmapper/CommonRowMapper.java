package ru.rtlabs.rowmapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;


public abstract class CommonRowMapper<T> implements RowMapper<T>
{
    protected Integer getInteger(ResultSet resultSet, String columnName) throws SQLException
    {
        Integer result = resultSet.getInt(columnName);
        if (resultSet.wasNull())
        {
            result = null;
        }
        return result;
    }

    protected String getString(ResultSet resultSet, String columnName) throws SQLException
    {
        String result = resultSet.getString(columnName);
        if (resultSet.wasNull())
        {
            result = null;
        }
        return result;
    }


    protected Date getDate(ResultSet resultSet, String columnName) throws SQLException
    {
        Date result = resultSet.getDate(columnName);
        if (resultSet.wasNull())
        {
            result = null;
        }
        return result;
    }
}
