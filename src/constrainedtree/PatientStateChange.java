/*
 * Copyright (C) 2019. Tim Vaughan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package constrainedtree;

public class PatientStateChange {
    public String patientName;
    public String state;
    public String stateValue;
    public double time;
    public String dateString;

    public enum Type { ON, OFF }

    public Type type;

    public PatientStateChange(String patientName, String state, String stateValue,
                              double time, String dateString,
                              Type type) {
        this.patientName = patientName;
        this.state = state;
        this.stateValue = stateValue;
        this.time = time;
        this.dateString = dateString;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Patient:" + patientName + " State:" + state
                + " Value:" + stateValue + " (" + type + " " + dateString + ") ";
    }
}
