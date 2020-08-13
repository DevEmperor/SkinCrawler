package main;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class Main {

	public static void main(String[] args) {
		String searchedPlayer = JOptionPane.showInputDialog("Please enter the name of a Minecraft-Player!");
		if (searchedPlayer == null || (searchedPlayer != null && ("".equals(searchedPlayer)))) {
			System.exit(0);
		} else {
			try {
				if(getValueFromJSON(getValueFromAPI(new URL("https://status.mojang.com/check")), "api.mojang.com").equals("red")) {
					JOptionPane.showMessageDialog(null, "The Mojang API-Server is currently not working. Please try again later!");
					System.exit(1);
				} else {
					String usernameResponse = getValueFromAPI(new URL("https://api.mojang.com/users/profiles/minecraft/" + searchedPlayer));
					
					String uuid = getValueFromJSON(usernameResponse, "id");
					String profileResponse = getValueFromAPI(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid));
					URL skinUrl = new URL(getValueFromJSON(new String(
							Base64.getDecoder().decode(getValueFromJSON(profileResponse, "value").getBytes())), "url"));
					
					InputStream in = skinUrl.openStream();
					Files.copy(in, new File(System.getProperty("java.io.tmpdir") + File.separator + searchedPlayer).toPath(), 
							StandardCopyOption.REPLACE_EXISTING);
					
					BufferedImage finalImage = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
					BufferedImage image = ImageIO.read(new File(System.getProperty("java.io.tmpdir") + File.separator + searchedPlayer));
					
					Graphics2D g2d = finalImage.createGraphics();
					g2d.drawImage(image.getSubimage(8, 8, 8, 8), 4, 0, null); //head
					g2d.drawImage(image.getSubimage(20, 20, 8, 12), 4, 8, null); //body
					
					if(image.getHeight() == 64) {
						g2d.drawImage(image.getSubimage(20, 52, 4, 12), 8, 20, null); //left-leg
						g2d.drawImage(image.getSubimage(4, 20, 4, 12), 4, 20, null); //right-leg
						g2d.drawImage(image.getSubimage(36, 52, 4, 12), 12, 8, null); //left-arm
						g2d.drawImage(image.getSubimage(44, 20, 4, 12), 0, 8, null); //right-arm
					} else if(image.getHeight() == 32) {
						g2d.drawImage(image.getSubimage(4, 20, 4, 12), 4, 20, null); //right-leg
						g2d.drawImage(flipImage(image.getSubimage(4, 20, 4, 12)), 8, 20, null); //left-leg
						g2d.drawImage(image.getSubimage(44, 20, 4, 12), 0, 8, null); //right-arm
						g2d.drawImage(flipImage(image.getSubimage(44, 20, 4, 12)), 12, 8, null); //left-arm
					}
					
					int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
					int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
					
					JFrame frame = new JFrame(searchedPlayer);
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.add(new JLabel(new ImageIcon(finalImage.getScaledInstance(screenWidth / 5, screenHeight / 2, 
							BufferedImage.SCALE_SMOOTH))), BorderLayout.CENTER);
					frame.setBounds(0, 0, screenWidth / 4, (int) (screenHeight / 1.7));
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Exception occured: " + e.getMessage());
				System.exit(1);
			}
		}
	}
	
	private static BufferedImage flipImage(BufferedImage image) {
		AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
		tx.translate(-image.getWidth(null), 0);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		image = op.filter(image, null);
		return image;
	}
	
	private static String getValueFromAPI(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		int responseCode = connection.getResponseCode();
		
		if(responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String readLine = null;
			StringBuffer response = new StringBuffer();
			
			while ((readLine = in.readLine()) != null) {
				response.append(readLine);
			}
			in.close();
			
			return response.toString();
		} else {
			throw new IOException("Error while connecting to the Mojang-API-Server. \n"
					+ "Maybe you entered a player name that doesn't exist or your internet connection is down?");
		}
	}
	
	private static String getValueFromJSON(String response, String property) {
		StringBuilder text = new StringBuilder(response);
		int start;
		int startValue = text.indexOf("\"" + property + "\":\"");
		if(startValue == -1) {
			startValue = text.indexOf("\"" + property + "\" : \"");
			start = startValue + 6 + property.length();
		} else {
			start = startValue + 4 + property.length();
		}
		return text.substring(start, text.indexOf("\"", start));
	}

}
