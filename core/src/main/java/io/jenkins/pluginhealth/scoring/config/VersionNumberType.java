/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jenkins.pluginhealth.scoring.config;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import hudson.util.VersionNumber;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

public class VersionNumberType implements UserType<VersionNumber> {
    @Override
    public int getSqlType() {
        return SqlTypes.VARCHAR;
    }

    @Override
    public Class<VersionNumber> returnedClass() {
        return VersionNumber.class;
    }

    @Override
    public boolean equals(VersionNumber x, VersionNumber y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(VersionNumber x) {
        return Objects.hash(x);
    }

    @Override
    public VersionNumber nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        final String value = rs.getString(position);
        return Objects.isNull(value) ? null : new VersionNumber(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, VersionNumber value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (Objects.isNull(value)) {
            st.setNull(index, SqlTypes.VARCHAR);
        } else {
            st.setString(index, value.toString());
        }
    }

    @Override
    public VersionNumber deepCopy(VersionNumber value) {
        return Objects.isNull(value) ? null : new VersionNumber(value.toString());
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(VersionNumber value) {
        final VersionNumber versionNumber = deepCopy(value);
        return versionNumber == null ? null : versionNumber.toString();
    }

    @Override
    public VersionNumber assemble(Serializable cached, Object owner) {
        return deepCopy((VersionNumber) cached);
    }

    @Override
    public VersionNumber replace(VersionNumber detached, VersionNumber managed, Object owner) {
        return deepCopy(detached);
    }
}
