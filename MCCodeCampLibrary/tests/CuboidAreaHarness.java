package me.simoncrafter.mCCodeCampLibrary.internal.editor.area;

import java.util.List;
import org.bukkit.util.Vector;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;

/** Exercises the real draft model without a server. */
public class CuboidAreaHarness {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        CuboidAreaSelection selection = new CuboidAreaSelection(List.of());
        check(!selection.selectCorner(new Vector(-.2, 4.9, 8.1)), "first corner is pending");
        check(selection.selectCorner(new Vector(1.9, 3.1, 8.8)), "second corner appends");
        OpenableRegion first = selection.getAreas().getFirst();
        check(first.getMin().equals(new Vector(-1, 3, 8)), "negative coordinates floor");
        check(first.getMax().equals(new Vector(1, 4, 8)) && first.getBlockCount() == 6, "inclusive blocks");
        selection.selectCorner(new Vector(20, 20, 20));
        selection.selectCorner(new Vector(20, 20, 20));
        check(selection.getAreas().size() == 2, "independent areas retained");
        check(CuboidAreaSelection.center(first).equals(new Vector(.5, 4, 8.5)), "outline center includes full final block");
        check(CuboidAreaSelection.size(first).equals(new Vector(3, 2, 1)), "outline dimensions inclusive");
        selection.getAreas().clear();
        check(selection.getAreas().size() == 2, "snapshot cannot mutate draft");
        selection.selectCorner(new Vector(0, 0, 0));
        selection.cancelCorner();
        check(!selection.selectCorner(new Vector(2, 2, 2)), "cancel clears pending corner");
        selection.cancelCorner();
        selection.removeArea(0);
        check(selection.getAreas().size() == 1 && first.getMin().getX() == -1, "remove only requested area");
        selection.clear();
        check(selection.getAreas().isEmpty(), "clear draft");
        Vector origin=new Vector(100,64,-200);
        var absolute=CuboidAreaSelection.translate(List.of(first),origin);
        check(absolute.getFirst().getMin().equals(first.getMin().add(origin)),"legacy relative areas become world corners");
        var restored=CuboidAreaSelection.translate(absolute,origin.clone().multiply(-1));
        check(restored.getFirst().getMin().equals(first.getMin()) && restored.getFirst().getMax().equals(first.getMax()),"world selection roundtrips to legacy door offsets");
        System.out.println("CuboidAreaHarness: 12 behavior checks passed");
    }
}
