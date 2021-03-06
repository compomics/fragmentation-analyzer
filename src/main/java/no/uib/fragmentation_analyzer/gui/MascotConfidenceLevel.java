package no.uib.fragmentation_analyzer.gui;

import javax.swing.JOptionPane;

/**
 * A simple dialog that lets the user provide the Mascot confidence level.
 *
 * @author Harald Barsnes
 */
public class MascotConfidenceLevel extends javax.swing.JDialog {

    private DataSource dataSourceDialog;

    /**
     * Creates a new MascotConfidenceLevel dialog.
     *
     * @param dataSourceDialog
     * @param modal
     */
    public MascotConfidenceLevel(DataSource dataSourceDialog, boolean modal) {
        super(dataSourceDialog, modal);
        initComponents();

        confidenceLevelJSpinner.setValue(
                new Integer((int) (dataSourceDialog.getFragmentationAnalyzer().getUserProperties().getMascotConfidenceLevel()*100)));

        this.dataSourceDialog = dataSourceDialog;

        setLocationRelativeTo(dataSourceDialog);
        setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okJButton = new javax.swing.JButton();
        cancelJButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        confidenceLevelJSpinner = new javax.swing.JSpinner();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Mascot Confidence Level");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        okJButton.setText("OK");
        okJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okJButtonActionPerformed(evt);
            }
        });

        cancelJButton.setText("Cancel");
        cancelJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelJButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Select the Mascot confidence level to use:");

        confidenceLevelJSpinner.setModel(new javax.swing.SpinnerNumberModel(95, 0, 100, 1));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .add(okJButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelJButton))
                    .add(layout.createSequentialGroup()
                        .add(jLabel2)
                        .add(18, 18, 18)
                        .add(confidenceLevelJSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(new java.awt.Component[] {cancelJButton, okJButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(confidenceLevelJSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancelJButton)
                    .add(okJButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Verifies the selected confidence level and then closes the dialog.
     *
     * @param evt
     */
    private void okJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okJButtonActionPerformed
        
        try{
            dataSourceDialog.getFragmentationAnalyzer().getUserProperties().setMascotConfidenceLevel(
                    ((Integer) confidenceLevelJSpinner.getValue()).doubleValue() / 100);
            setVisible(true);
            dispose();
        } catch(NumberFormatException e){
            JOptionPane.showMessageDialog(this,
                    "Confidence level has to be an integer.",
                    "Confidence Level", JOptionPane.INFORMATION_MESSAGE);
            confidenceLevelJSpinner.requestFocus();
        }

}//GEN-LAST:event_okJButtonActionPerformed

    /**
     * Closes the dialog and terminates the import process.
     *
     * @param evt
     */
    private void cancelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelJButtonActionPerformed
        dataSourceDialog.cancelProgress();
        setVisible(true);
        dispose();
}//GEN-LAST:event_cancelJButtonActionPerformed

    /**
     * @see #cancelJButtonActionPerformed(java.awt.event.ActionEvent) 
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        cancelJButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelJButton;
    private javax.swing.JSpinner confidenceLevelJSpinner;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton okJButton;
    // End of variables declaration//GEN-END:variables
}
