package ru.rtlabs.rowmapper;

import ru.rtlabs.Entity.Employe;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DocRowMapper extends CommonRowMapper<Employe> {

    @Override
    public Employe mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        Employe employe = new Employe();
        employe.setId(getInteger(rs, "indiv_id"));
        employe.setSnils(getString(rs, "number"));
        return employe;

    }
}