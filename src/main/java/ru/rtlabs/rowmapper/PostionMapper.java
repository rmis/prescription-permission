package ru.rtlabs.rowmapper;

import ru.rtlabs.Entity.Employe;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by leon4uk on 13.01.17.
 */
public class PostionMapper   extends CommonRowMapper<Employe> {

    @Override
    public Employe mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        Employe employe = new Employe();
        employe.setPositionId(getInteger(rs, "id"));
        return employe;
    }
}
